/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lockstep;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
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
import java.util.concurrent.Semaphore;
import lockstep.messages.handshake.*;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 *
 * @author Raff
 */
public class LockstepServer<Command extends Serializable> implements Runnable
{
    volatile ConcurrentSkipListSet<Integer> hostIDs;
    
    /**
     * Used without interframe times. As soon as all inputs for a frame are 
     * available, they're forwarded to all the clients
     */
    volatile Map<Integer, ServerReceivingQueue<Command>> serverQueues;
    
    /**
     * Buffers for frame input to send to clients. 
     * For each client partecipating in the session there's a queue for each of
     * the other clients.
     */
    volatile Map<Integer, Map<Integer, TransmissionQueue<Command>>> transmissionFrameQueueTree;
    
    
    volatile Map<Integer, ACKQueue> ackQueues;
    
    /**
     * Threads used for receiving and transmitting of frames. 
     * A pair for each client partecipating in the session.
     */
    Map<Integer, Thread> receivers;
    Map<Integer, Thread> transmitters;

    /**
     * Used for synchronization between server and executionFrameQueues
     */
    volatile Map<Integer, Boolean> executionQueuesHeadsAvailability;
    
    //volatile CyclicCountDownLatch cyclicExecutionLatch;
    Semaphore executionSemaphore;
    
    int tcpPort;
    int clientsNumber;
    
    private static final Logger LOG = LogManager.getLogger(LockstepServer.class);
    private final int tickrate;
    
    public LockstepServer(int tcpPort, int clientsNumber, int tickrate)
    {
        this.tcpPort = tcpPort;
        this.clientsNumber = clientsNumber;
        this.tickrate = tickrate;
        
        receivers = new HashMap<>();
        transmitters = new HashMap<>();
        
        //cyclicExecutionLatch = new CyclicCountDownLatch(clientsNumber);
        executionSemaphore = new Semaphore(0);
        serverQueues = new ConcurrentHashMap<>();
        transmissionFrameQueueTree = new ConcurrentHashMap<>();
        ackQueues = new ConcurrentHashMap<>();
        hostIDs = new ConcurrentSkipListSet<>();
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
        atServerStarted();
        handshake();
        atHandshakeEnded();
        
        while(true)
        {
            try
            {
                
                //debugSimulation();
                
                for(ReceivingQueue exQ : serverQueues.values())
                    LOG.debug(exQ);
                
                //Wait that everyone has received current frame
                LOG.debug("Waiting the receivingQueues to forward");
                executionSemaphore.acquire();
                LOG.debug("ReceivingQueues ready for forwarding");
                
                Map<Integer, FrameInput> commands = collectCommands();
                distributeCommands(commands);
                LOG.debug("Message batch forwarded");
            } catch (InterruptedException ex)
            {
                LOG.fatal("Server interrupted while waiting for frames");
                ex.printStackTrace();
                System.exit(1);
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
                LOG.info("Connection " + i + " accepted from " +  tcpConnectionSocket.getInetAddress().getHostAddress());
                Thread handshake = new Thread(() -> clientHandshake(tcpConnectionSocket, firstFrameNumber, barrier, latch));
                handshake.start();
            }
            latch.await();
            LOG.info("All handshakes completed");
        } catch (IOException | InterruptedException ex)
        {
            LOG.fatal("Error in handshake " + ex.getMessage());
            ex.printStackTrace();
            System.exit(1);
        }
    }
    
    private void clientHandshake(Socket tcpSocket, int firstFrameNumber, CyclicBarrier barrier, CountDownLatch latch)
    {
        LOG.debug("ClientHandshake started");
        try(ObjectOutputStream oout = new ObjectOutputStream(tcpSocket.getOutputStream());)
        {
            oout.flush();
            LOG.debug("oout flushed");
            try(ObjectInputStream oin = new ObjectInputStream(tcpSocket.getInputStream());)
            {
                //Receive hello message from client and replyess());
                LOG.info("Waiting an hello from " + tcpSocket.getInetAddress().getHostAddress());
                oout.flush();
                ClientHello hello = (ClientHello) oin.readObject();
                LOG.info("Received an hello from " + tcpSocket.getInetAddress().getHostAddress());
                DatagramSocket udpSocket = new DatagramSocket();
                InetSocketAddress clientUDPAddress = new InetSocketAddress(tcpSocket.getInetAddress().getHostAddress(), hello.clientUDPPort);
                udpSocket.connect(clientUDPAddress);
                //TO DO: review timeout settings
                udpSocket.setSoTimeout(5000);

                int assignedHostID;
                do{
                    assignedHostID = (new Random()).nextInt(100000) + 10000;
                    LOG.debug("Extracted ID is " + assignedHostID);
                }while(!this.hostIDs.add(assignedHostID));

                LOG.info("Assigned hostID " + assignedHostID + " to " + tcpSocket.getInetAddress().getHostAddress() + ", sending helloReply");
                ServerHelloReply helloReply = new ServerHelloReply(udpSocket.getLocalPort(), assignedHostID, clientsNumber, firstFrameNumber);
                oout.writeObject(helloReply);

                Map<Integer, TransmissionQueue<Command>> clientTransmissionFrameQueues = new HashMap<>();
                this.transmissionFrameQueueTree.put(assignedHostID, clientTransmissionFrameQueues);
                
                ACKQueue clientAckQueue = new ACKQueue();
                ackQueues.put(assignedHostID, clientAckQueue);
                
                clientReceiveSetup(assignedHostID, udpSocket, firstFrameNumber, clientTransmissionFrameQueues);

                LOG.debug("Waiting at first barrier for " + assignedHostID);
                barrier.await();

                //Send second reply
                ClientsAnnouncement announcement = new ClientsAnnouncement();
                announcement.hostIDs = ArrayUtils.toPrimitive(this.hostIDs.toArray(new Integer[0]));
                LOG.debug("Sending clientsAnnouncement for " + assignedHostID);
                oout.writeObject(announcement);
                
                clientTransmissionSetup(assignedHostID, firstFrameNumber, udpSocket, clientTransmissionFrameQueues);

                //Wait for other handshakes to reach final step
                LOG.debug("Waiting at second barrier for " + assignedHostID);
                barrier.await();
                oout.writeObject(new SimulationStart());   

                //Continue with execution
                latch.countDown();
            }
        } catch (IOException | ClassNotFoundException | InterruptedException | BrokenBarrierException ex)
        {
            LOG.fatal("Exception at clientHandshake " + ex.getMessage());
            ex.printStackTrace();
            System.exit(1);
        }            
    }
    
    private void clientReceiveSetup(int clientID, DatagramSocket clientUDPSocket, int initialFrameNumber, Map<Integer, TransmissionQueue<Command>> transmissionFrameQueues)
    {
        ServerReceivingQueue receivingQueue = new ServerReceivingQueue(initialFrameNumber, clientID, executionSemaphore);
        this.serverQueues.put(clientID, receivingQueue);
        HashMap<Integer,ServerReceivingQueue> receivingQueueWrapper = new HashMap<>();
        receivingQueueWrapper.put(clientID, receivingQueue);
        LockstepReceiver receiver = new LockstepReceiver(clientUDPSocket, tickrate, receivingQueueWrapper, transmissionFrameQueues, "Receiver-from-"+clientID, ackQueues.get(clientID));
        Thread receiverThread = new Thread(receiver);
        receivers.put(clientID, receiverThread);
        receiverThread.start();
    }
    
    private void clientTransmissionSetup(int clientID, int firstFrameNumber, DatagramSocket udpSocket, Map<Integer, TransmissionQueue<Command>> clientTransmissionFrameQueues)
    {
        //System.out.println("Setting up transmission to client " + clientID);
        for(int hostID : hostIDs)
        {
            if(hostID != clientID)
            {
                TransmissionQueue transmissionFrameQueue = new TransmissionQueue(firstFrameNumber, hostID);
                clientTransmissionFrameQueues.put(hostID, transmissionFrameQueue);
            }
        }
        LockstepTransmitter transmitter = new LockstepTransmitter(udpSocket, tickrate, 0, clientTransmissionFrameQueues, "Transmitter-to-"+clientID, ackQueues.get(clientID));
        Thread transmitterThread = new Thread(transmitter);
        transmitters.put(clientID, transmitterThread);
        transmitterThread.start();
        
    }
    
    private Map<Integer, FrameInput> collectCommands()
    {        
        Map<Integer, FrameInput> nextCommands = new TreeMap<>();
        boolean foundFirstFrame = false;
        for(Entry<Integer, ServerReceivingQueue<Command>> serverQueueEntry : this.serverQueues.entrySet())
        {
            Integer senderID = serverQueueEntry.getKey();
            FrameInput frame = serverQueueEntry.getValue().pop();
            if(frame != null)
            {
                nextCommands.put(senderID, frame);     
                if(!foundFirstFrame)
                {
                    foundFirstFrame = true;
                }
                else
                {
                    executionSemaphore.tryAcquire();
                }
            }
        }
        return nextCommands;
    }
    
    private void distributeCommands(Map<Integer, FrameInput> nextFrameInputs)
    {
        //For each command
        for(Entry<Integer, FrameInput> frameEntry : nextFrameInputs.entrySet())
        {
            Integer senderID = frameEntry.getKey();

            //For each client, take its tree of transmission queues
            for(Entry<Integer, Map<Integer, TransmissionQueue<Command>>> transmissionFrameQueueMapEntry : this.transmissionFrameQueueTree.entrySet())
            {
                Integer recipientID = transmissionFrameQueueMapEntry.getKey();
                
                //If the frameInput doesn't come from that client, forward the frameInput though the correct transmission queue
                if(!recipientID.equals(senderID))
                {
                    Map<Integer, TransmissionQueue<Command>> recipientTransmissionQueueMap = transmissionFrameQueueMapEntry.getValue();
                    TransmissionQueue<Command> transmissionFrameQueueFromSender = recipientTransmissionQueueMap.get(senderID);
                    transmissionFrameQueueFromSender.push(frameEntry.getValue());
                }
            }
        }
    }

    /**
     * Optionally extended. Called before the handshake phase.
     */
    protected void atServerStarted()
    {
    }
    
    /**
     * Optionally extended. Called after the handshake phase.
     */
    protected void atHandshakeEnded()
    {
    }
    
    public void debugSimulation()
    {
        
        //System.out.println("EXECUTION QUEUES");
        for(Entry<Integer, ServerReceivingQueue<Command>> exeFrameQueues : serverQueues.entrySet())
        {
            //System.out.println(exeFrameQueues);
        }
        
        //System.out.println("TRANSMISSION QUEUES");
        for(Entry<Integer, Map<Integer, TransmissionQueue<Command>>> transmissionMap : transmissionFrameQueueTree.entrySet())
        {
            //System.out.println("Transmission Queues to " + transmissionMap.getKey());
            
            for(Entry<Integer, TransmissionQueue<Command>> txQ : transmissionMap.getValue().entrySet())
            {
                //System.out.println(txQ);
            }
        }
        
        
    }
}
