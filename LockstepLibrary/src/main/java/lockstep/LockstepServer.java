/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lockstep;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import lockstep.messages.handshake.*;
import org.apache.commons.lang3.ArrayUtils;

/**
 *
 * @author Raff
 */
public class LockstepServer implements Runnable
{
    ConcurrentSkipListSet<Integer> hostIDs;
    
    /**
     * Used without interframe times. As soon as all inputs for a frame are 
     * available, they're forwarded to all the clients
     */
    Map<Integer, ExecutionFrameQueue> executionFrameQueues;
    
    /**
     * Buffers for frame input to send to clients. 
     * For each client partecipating in the session there's a queue for each of
     * the other clients.
     */
    Map<Integer, Map<Integer, TransmissionFrameQueue>> transmissionFrameQueueTree;
    
    /**
     * Threads used for receiving and transmitting of frames. 
     * A pair for each client partecipating in the session.
     */
    Map<Integer, LockstepTransmitter> transmitters;
    Map<Integer, LockstepReceiver> receivers;

    /**
     * Used for synchronization between server and executionFrameQueues
     */
    Map<Integer, Boolean> executionQueuesHeadsAvailability;
    
    CyclicCountDownLatch inputLatch;
    
    int tcpPort;
    int clientsNumber;
    
    ExecutorService executorService;
    
    static final int executionBufferSize = 1024; //Serve davvero??
    
    public LockstepServer(int tcpPort, int clientsNumber)
    {
        this.tcpPort = tcpPort;
        this.clientsNumber = clientsNumber;
        executorService = Executors.newWorkStealingPool();
        
        inputLatch = new CyclicCountDownLatch(clientsNumber);
        executionFrameQueues = new ConcurrentHashMap<>();
        transmissionFrameQueueTree = new ConcurrentHashMap<>();
    }

    /**
     * The server simply cycles collecting a complete set of frame inputs and
     * forwarding them to all the clients. Differently from the clients, it doesn't
     * wait any interframe time to process the executionFrameQueues.
     * If a frame lacks any input from any client, the server stops and waits for
     * them eventually forcing the clients to stop for synchronization.
     */
    @Override
    public void run()
    {
        handshake();
        
        while(true)
        {
            try
            {
                //Wait that everyone has received current frame
                inputLatch.await();
                
                Map<Integer, FrameInput> frameInputs = collectFrameInputs();
                distributeFrameInputs(frameInputs);
            } catch (InterruptedException ex)
            {
                Logger.getLogger(LockstepServer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    private void handshake()
    {
        try(
            ServerSocket tcpServerSocket = new ServerSocket(tcpPort);
        )
        {
            CyclicBarrier barrier = new CyclicBarrier(this.clientsNumber);
            CountDownLatch latch = new CountDownLatch(this.clientsNumber);
            int firstFrameNumber = (new Random()).nextInt(1000) + 100;
            
            for(int i = 0; i < clientsNumber; i++)
            {
                Socket tcpConnectionSocket = tcpServerSocket.accept();
                executorService.submit(() -> clientHandshake(tcpConnectionSocket, firstFrameNumber, barrier, latch));
            }
            latch.await();
        } catch (IOException ex)
        {
            Logger.getLogger(LockstepServer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex)
        {
            Logger.getLogger(LockstepServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void clientHandshake(Socket tcpSocket, int firstFrameNumber, CyclicBarrier barrier, CountDownLatch latch)
    {
        try(
            ObjectOutputStream oout = new ObjectOutputStream(tcpSocket.getOutputStream());
            ObjectInputStream oin = new ObjectInputStream(tcpSocket.getInputStream()); 
        )
        {
            //Receive hello message from client and reply
            ClientHello hello = (ClientHello) oin.readObject();
            
            DatagramSocket udpSocket = new DatagramSocket();
            udpSocket.bind(null);
            InetSocketAddress clientUDPAddress = new InetSocketAddress(tcpSocket.getInetAddress().getHostAddress(), hello.clientUDPPort);
            udpSocket.connect(clientUDPAddress);

            int assignedHostID;
            do{
                assignedHostID = (new Random()).nextInt(100000) + 10000;
            }   while(!this.hostIDs.add(assignedHostID));
            
            ServerHelloReply helloReply = new ServerHelloReply(udpSocket.getLocalPort(), assignedHostID, clientsNumber, firstFrameNumber);
            oout.writeObject(helloReply);
            
            Map<Integer, TransmissionFrameQueue> clientTransmissionFrameQueues = new HashMap<>();
            this.transmissionFrameQueueTree.put(assignedHostID, clientTransmissionFrameQueues);
            clientReceiveSetup(assignedHostID, udpSocket, firstFrameNumber, clientTransmissionFrameQueues);
            
            barrier.await();
                        
            //Send second reply
            ClientsAnnouncement announcement = new ClientsAnnouncement();
            announcement.hostIDs = ArrayUtils.toPrimitive(this.hostIDs.toArray(new Integer[0]));
            oout.writeObject(announcement);
            
            clientTransmissionSetup(assignedHostID, firstFrameNumber, udpSocket, clientTransmissionFrameQueues);
            
            //Wait for other handshakes to reach final step
            barrier.await();
            oout.writeObject(new SimulationStart());   

            //Continue with execution
            latch.countDown();
        } catch (IOException ex)
        {
            Logger.getLogger(LockstepServer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex)
        {
            Logger.getLogger(LockstepServer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex)
        {
            Logger.getLogger(LockstepServer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (BrokenBarrierException ex)
        {
            Logger.getLogger(LockstepServer.class.getName()).log(Level.SEVERE, null, ex);
        }            
    }
    
    private void clientReceiveSetup(int clientID, DatagramSocket clientUDPSocket, int initialFrameNumber, Map<Integer, TransmissionFrameQueue> transmissionFrameQueues)
    {
        ExecutionFrameQueue receivingQueue = new ExecutionFrameQueue(executionBufferSize, initialFrameNumber, clientID, inputLatch);
        this.executionFrameQueues.put(clientID, receivingQueue);
        HashMap<Integer,ExecutionFrameQueue> receivingQueueWrapper = new HashMap<>();
        receivingQueueWrapper.put(clientID, receivingQueue);
        LockstepReceiver receiver = new LockstepReceiver(clientUDPSocket, receivingQueueWrapper, transmissionFrameQueues);
        executorService.submit(receiver);
    }
    
    private void clientTransmissionSetup(int clientID, int firstFrameNumber, DatagramSocket udpSocket, Map<Integer, TransmissionFrameQueue> clientTransmissionFrameQueues)
    {
        for(int hostID : hostIDs)
        {
            if(hostID != clientID)
            {
                TransmissionFrameQueue transmissionFrameQueue = new TransmissionFrameQueue(firstFrameNumber);
                clientTransmissionFrameQueues.put(hostID, transmissionFrameQueue);
            }
        }
        LockstepTransmitter transmitter = new LockstepTransmitter(udpSocket, clientTransmissionFrameQueues);
        executorService.submit(transmitter);
    }
    
    private Map<Integer, FrameInput> collectFrameInputs()
    {
        Map<Integer, FrameInput> frameInputs = new TreeMap<>();
        for(Entry<Integer, ExecutionFrameQueue> entry : this.executionFrameQueues.entrySet())
        {
            frameInputs.put(entry.getKey(), entry.getValue().pop());
        }
        return frameInputs;
    }
    
    private void distributeFrameInputs(Map<Integer, FrameInput> frameInputs)
    {
        //For each frameInput
        for(Entry<Integer, FrameInput> frameInputEntry : frameInputs.entrySet())
        {
            //For each client, take its tree of transmission queues
            for(Entry<Integer, Map<Integer, TransmissionFrameQueue>> transmissionFrameQueueMapEntry : this.transmissionFrameQueueTree.entrySet())
            {
                //If the frameInput doesn't come from that client, forward the frameInput though the correct transmission queue
                if(transmissionFrameQueueMapEntry.getKey() != frameInputEntry.getKey())
                {
                    Map<Integer, TransmissionFrameQueue> transmissionFrameQueueMap = transmissionFrameQueueMapEntry.getValue();
                    TransmissionFrameQueue transmissionFrameQueue = transmissionFrameQueueMap.get(frameInputEntry.getKey());
                    transmissionFrameQueue.push(frameInputEntry.getValue());
                }
            }
        }
    }
}
