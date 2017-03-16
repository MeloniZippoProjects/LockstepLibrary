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
import org.apache.log4j.Logger;

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
    volatile Map<Integer, ExecutionFrameQueue<Command>> executionFrameQueues;
    
    /**
     * Buffers for frame input to send to clients. 
     * For each client partecipating in the session there's a queue for each of
     * the other clients.
     */
    volatile Map<Integer, Map<Integer, TransmissionFrameQueue<Command>>> transmissionFrameQueueTree;
    
    /**
     * Threads used for receiving and transmitting of frames. 
     * A pair for each client partecipating in the session.
     */
    ExecutorService transmitters;
    ExecutorService receivers;

    /**
     * Used for synchronization between server and executionFrameQueues
     */
    volatile Map<Integer, Boolean> executionQueuesHeadsAvailability;
    
    volatile CyclicCountDownLatch cyclicExecutionLatch;
    
    int tcpPort;
    int clientsNumber;
    
    private static final Logger LOG = Logger.getLogger(LockstepServer.class.getName());
    private final int tickrate;
    
    public LockstepServer(int tcpPort, int clientsNumber, int tickrate)
    {
        this.tcpPort = tcpPort;
        this.clientsNumber = clientsNumber;
        this.tickrate = tickrate;
    
        transmitters = Executors.newFixedThreadPool(clientsNumber);
        receivers = Executors.newFixedThreadPool(clientsNumber);
        
        cyclicExecutionLatch = new CyclicCountDownLatch(clientsNumber);
        executionFrameQueues = new ConcurrentHashMap<>();
        transmissionFrameQueueTree = new ConcurrentHashMap<>();
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
                for(ExecutionFrameQueue exQ : executionFrameQueues.values())
                    LOG.debug(exQ);
                
                //Wait that everyone has received current frame
                LOG.debug("Waiting the receivingQueues to forward");
                cyclicExecutionLatch.await();
                LOG.debug("ReceivingQueues ready for forwarding");
                
                Map<Integer, Command> commands = collectCommands();
                distributeCommands(commands);
                LOG.debug("Message batch forwarded");
            } catch (InterruptedException ex)
            {
                LOG.fatal("Server interrupted while waiting for frames");
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
            ExecutorService handshakes = Executors.newFixedThreadPool(clientsNumber);
            int firstFrameNumber = (new Random()).nextInt(1000) + 100;
            
            for(int i = 0; i < clientsNumber; i++)
            {
                Socket tcpConnectionSocket = tcpServerSocket.accept();
                LOG.info("Connection " + i + " accepted from " +  tcpConnectionSocket.getInetAddress().getHostAddress());
                handshakes.submit(() -> clientHandshake(tcpConnectionSocket, firstFrameNumber, barrier, latch));
            }
            latch.await();
            LOG.info("All handshakes completed");
        } catch (IOException | InterruptedException ex)
        {
            LOG.fatal("Error in handshake " + ex.getMessage());
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

                int assignedHostID;
                do{
                    assignedHostID = (new Random()).nextInt(100000) + 10000;
                    LOG.debug("Extracted ID is " + assignedHostID);
                }while(!this.hostIDs.add(assignedHostID));

                LOG.info("Assigned hostID " + assignedHostID + " to " + tcpSocket.getInetAddress().getHostAddress() + ", sending helloReply");
                ServerHelloReply helloReply = new ServerHelloReply(udpSocket.getLocalPort(), assignedHostID, clientsNumber, firstFrameNumber);
                oout.writeObject(helloReply);

                Map<Integer, TransmissionFrameQueue<Command>> clientTransmissionFrameQueues = new HashMap<>();
                this.transmissionFrameQueueTree.put(assignedHostID, clientTransmissionFrameQueues);
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
            System.exit(1);
        }            
    }
    
    private void clientReceiveSetup(int clientID, DatagramSocket clientUDPSocket, int initialFrameNumber, Map<Integer, TransmissionFrameQueue<Command>> transmissionFrameQueues)
    {
        ExecutionFrameQueue receivingQueue = new ExecutionFrameQueue(initialFrameNumber, clientID, cyclicExecutionLatch);
        this.executionFrameQueues.put(clientID, receivingQueue);
        HashMap<Integer,ExecutionFrameQueue> receivingQueueWrapper = new HashMap<>();
        receivingQueueWrapper.put(clientID, receivingQueue);
        LockstepReceiver receiver = new LockstepReceiver(clientUDPSocket, receivingQueueWrapper, transmissionFrameQueues, "Receiver-from-"+clientID);
        receivers.submit(receiver);
    }
    
    private void clientTransmissionSetup(int clientID, int firstFrameNumber, DatagramSocket udpSocket, Map<Integer, TransmissionFrameQueue<Command>> clientTransmissionFrameQueues)
    {
        Semaphore transmissionSemaphore = new Semaphore(0);
        for(int hostID : hostIDs)
        {
            if(hostID != clientID)
            {
                TransmissionFrameQueue transmissionFrameQueue = new TransmissionFrameQueue(firstFrameNumber, transmissionSemaphore, hostID);
                clientTransmissionFrameQueues.put(hostID, transmissionFrameQueue);
            }
        }
        LockstepTransmitter transmitter = new LockstepTransmitter(udpSocket, tickrate ,clientTransmissionFrameQueues, transmissionSemaphore, "Transmitter-to-"+clientID);
        transmitters.submit(transmitter);
    }
    
    private Map<Integer, Command> collectCommands()
    {        
        Map<Integer, Command> nextCommands = new TreeMap<>();
        for(Entry<Integer, ExecutionFrameQueue<Command>> executionFrameQueueEntry : this.executionFrameQueues.entrySet())
        {
            Integer senderID = executionFrameQueueEntry.getKey();
            Command command = executionFrameQueueEntry.getValue().pop();
            if(command == null)
                System.out.println("ERRORE MADORNALE DIO POVERO GESU MADONNA NEGRA");
            nextCommands.put(senderID, command);
        }
        return nextCommands;
    }
    
    private void distributeCommands(Map<Integer, Command> nextCommands)
    {
        //For each command
        for(Entry<Integer, Command> commandEntry : nextCommands.entrySet())
        {
            Integer senderID = commandEntry.getKey();

            //For each client, take its tree of transmission queues
            for(Entry<Integer, Map<Integer, TransmissionFrameQueue<Command>>> transmissionFrameQueueMapEntry : this.transmissionFrameQueueTree.entrySet())
            {
                Integer recipientID = transmissionFrameQueueMapEntry.getKey();
                
                //If the frameInput doesn't come from that client, forward the frameInput though the correct transmission queue
                if(!recipientID.equals(senderID))
                {
                    Map<Integer, TransmissionFrameQueue<Command>> recipientTransmissionQueueMap = transmissionFrameQueueMapEntry.getValue();
                    TransmissionFrameQueue<Command> transmissionFrameQueueFromSender = recipientTransmissionQueueMap.get(senderID);
                    transmissionFrameQueueFromSender.push(commandEntry.getValue());
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
}
