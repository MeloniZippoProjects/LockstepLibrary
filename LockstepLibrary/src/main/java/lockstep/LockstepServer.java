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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Semaphore;
import lockstep.messages.simulation.DisconnectionSignal;

import lockstep.messages.handshake.*;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class LockstepServer extends LockstepCoreThread
{
    ConcurrentSkipListSet<Integer> clientIDs;
    
    /**
     * Used without interframe times. As soon as all inputs for a frame are 
     * available, they're forwarded to all the clients
     */
    ConcurrentHashMap<Integer, ServerReceivingQueue> receivingQueues;
    private int connectionTimeout;
    
    /**
     * Buffers for frame input to send to clients. 
     * For each client partecipating in the session there's a queue for each of
     * the other clients.
     */
    ConcurrentHashMap<Integer, Map<Integer, TransmissionQueue>> transmissionFrameQueueTree;
    
    
    HashMap<Integer, ACKSet> ackQueues;
    
    /**
     * Threads used for receiving frames. 
     * The key is the ID of the host from which the thread receives frames
     */
    HashMap<Integer, Thread> receivers;
    
    /**
     * Threads used for transmitting frames.
     * The key is the ID of the host to which the frames are transmitted
     */
    HashMap<Integer, Thread> transmitters;
    
    //volatile CyclicCountDownLatch cyclicExecutionLatch;
    Semaphore executionSemaphore;
    
    int tcpPort;
    int clientsNumber;
    
    private static final Logger LOG = LogManager.getLogger(LockstepServer.class);
    private final int tickrate;
    
    private final List<DatagramSocket> openSockets;
  
    public static class Builder {

        private int tcpPort;
        private int clientsNumber;
        private int tickrate;
        private int connectionTimeout;

        private Builder() {
        }
        
        public Builder tcpPort(final int value) {
            this.tcpPort = value;
            return this;
        }

        public Builder clientsNumber(final int value) {
            this.clientsNumber = value;
            return this;
        }

        public Builder tickrate(final int value) {
            this.tickrate = value;
            return this;
        }
        
        public Builder connectionTimeout(final int value) {
            this.connectionTimeout = value;
            return this;
        }

        public LockstepServer build() {
            return new lockstep.LockstepServer(tcpPort, clientsNumber, tickrate, connectionTimeout);
        }
    }

    public static LockstepServer.Builder builder() {
        return new LockstepServer.Builder();
    }
    
    public LockstepServer(int tcpPort, int clientsNumber, int tickrate, int connectionTimeout)
    {
        //late fail left to Socket class
        this.tcpPort = tcpPort;
        
        if(clientsNumber <= 1)
            throw new IllegalArgumentException("clientsNumber must be at least 2");
        else
            this.clientsNumber = clientsNumber;
        
        if(tickrate <= 0)
            throw new IllegalArgumentException("Tickrate must be an integer greater than 0");
        else
            this.tickrate = tickrate;
        
        if(connectionTimeout < 0)
            throw new IllegalArgumentException("Connection timeout must be greater or equal than zero");
        else
            this.connectionTimeout = connectionTimeout;
        
        receivers = new HashMap<>();
        transmitters = new HashMap<>();
        
        executionSemaphore = new Semaphore(0);
        receivingQueues = new ConcurrentHashMap<>();
        transmissionFrameQueueTree = new ConcurrentHashMap<>();
        ackQueues = new HashMap<>();
        clientIDs = new ConcurrentSkipListSet<>();
        openSockets = new ArrayList<>();
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
        try{
            try{
                atServerStarted();
                handshakePhase();
                atHandshakeEnded();
            } catch(IOException ioEx)
            {
                LOG.fatal("Network exception during handshake");
                LOG.fatal(ioEx);
                return;
            }

            while(true)
            {
                //check if thread was interrupted, causing termination
                if(Thread.interrupted())
                    throw new InterruptedException();
                
                //Wait for any receveingQueue to have some frame to forward
                executionSemaphore.acquire();

                //Collect all the frames available and forward them
                Map<Integer, FrameInput> frameInputs = collectFrameInputs();
                forwardFrameInputs(frameInputs);
            }
        }
        catch(InterruptedException intEx)
        {
            otherCloseResources();
        }
    }
    
    /**
     * 
     */
    private void otherCloseResources()
    {
        for(Thread transmitter : transmitters.values())
            transmitter.interrupt();
        
        try
        {
            for(Thread receiver : receivers.values())
            {
                receiver.join();
            }

            for(Thread transmitter : transmitters.values())
            {
                transmitter.join();
            }
        }
        catch(InterruptedException intEx)
        {
            //shouldn't be interrupted
            LOG.fatal("Interrupted during termination!!");
            LOG.fatal(intEx);
        }
    }
    
    /**
     * Frees all resources tied to the server, that is networking threads and
     * sockets.
     */
    private void closeResources()
    {
        //Interrupts all network thread, causing their termination        
        for(Thread receiver : receivers.values())
        {
            receiver.interrupt();
        }
        
        for(Thread transmitter : transmitters.values())
        {
            transmitter.interrupt();
        }
        
        //Then waits for their effective termination
        try
        {
            for(Thread receiver : receivers.values())
            {
                receiver.join();
            }

            for(Thread transmitter : transmitters.values())
            {
                transmitter.join();
            }
        }
        catch(InterruptedException intEx)
        {
            //shouldn't be interrupted
            LOG.fatal("Interrupted during termination!!");
            LOG.fatal(intEx);
        }
        finally
        {
            //Eventually, close all sockets freeing their ports
            for(DatagramSocket udpSocket : openSockets)
            {
                udpSocket.close();
            }
        }
    }
    
    /**
     * This method puts the server in waiting for client connections. It returns
     * when the expected number of clients have successfully completed the 
     * handshake.
     * Parallel threads are started to handle the handshakes.
     * In case of failure, all threads are interrupted and then the exception is
     * propagated.
     * 
     * @throws IOException In case of failure on opening the ServerSocket and 
     * accepting connections through it 
     * @throws InterruptedException In case of failure during the handshake 
     * sessions
     */
    private void handshakePhase() throws IOException, InterruptedException
    {
        ServerSocket tcpServerSocket = new ServerSocket(tcpPort);
        
        CyclicBarrier barrier = new CyclicBarrier(this.clientsNumber);
        CountDownLatch latch = new CountDownLatch(this.clientsNumber);

        //Each session of the protocol starts with a different random frame number
        int firstFrameNumber = (new Random()).nextInt(1000) + 100;

        Thread[] handshakeSessions = new Thread[clientsNumber];
        
        for(int i = 0; i < clientsNumber; i++)
        {
            Socket tcpConnectionSocket = tcpServerSocket.accept();
            LOG.info("Connection " + i + " accepted from " +  tcpConnectionSocket.getInetAddress().getHostAddress());
            handshakeSessions[i] = new Thread(() -> serverHandshakeProtocol(tcpConnectionSocket, firstFrameNumber, barrier, latch, this));
            handshakeSessions[i].start();                
        }
        try{        
            latch.await();
        } catch(InterruptedException inEx) {
            for( Thread handshakeSession : handshakeSessions)
                handshakeSession.interrupt();
            
            for( Thread handshakeSession : handshakeSessions)
                handshakeSession.join();
            
            throw new InterruptedException();
        }        
        LOG.info("All handshakes completed");
    }
    
    /**
     * Implements the handshake protocol server side, setting up the UDP 
     * connection, queues and threads for a specific client.
     * To be run in parallel threads, one for each client, as they need
     * to synchronize to correctly setup the lockstep protocol.
     * It signals success through a latch or failure through interruption to the
     * server thread.
     * 
     * @param tcpSocket Connection with the client, to be used in handshake only
     * @param firstFrameNumber Frame number to initialize the lockstep protocol
     * @param barrier Used for synchronization with concurrent handshake sessions
     * @param latch Used to signal the successful completion of the handshake session.
     * @param server Used to signal failure of the handshake sessions, via interruption.
     */
    private void serverHandshakeProtocol(Socket tcpSocket, int firstFrameNumber, CyclicBarrier barrier, CountDownLatch latch, LockstepServer server)
    {
        LOG.debug("ClientHandshake started");
        try(ObjectOutputStream oout = new ObjectOutputStream(tcpSocket.getOutputStream());)
        {
            oout.flush();
            LOG.debug("oout flushed");
            try(ObjectInputStream oin = new ObjectInputStream(tcpSocket.getInputStream());)
            {
                //Receive hello message from client and reply
                LOG.info("Waiting an hello from " + tcpSocket.getInetAddress().getHostAddress());
                oout.flush();
                ClientHello hello = (ClientHello) oin.readObject();
                LOG.info("Received an hello from " + tcpSocket.getInetAddress().getHostAddress());
                DatagramSocket udpSocket = new DatagramSocket();
                openSockets.add(udpSocket);
                InetSocketAddress clientUDPAddress = new InetSocketAddress(tcpSocket.getInetAddress().getHostAddress(), hello.clientUDPPort);
                udpSocket.connect(clientUDPAddress);

                int assignedClientID;
                do{
                    assignedClientID = (new Random()).nextInt(100000) + 10000;
                }while(!this.clientIDs.add(assignedClientID));

                LOG.info("Assigned hostID " + assignedClientID + " to " + tcpSocket.getInetAddress().getHostAddress() + ", sending helloReply");
                ServerHelloReply helloReply = new ServerHelloReply(udpSocket.getLocalPort(), assignedClientID, clientsNumber, firstFrameNumber);
                oout.writeObject(helloReply);

                ConcurrentHashMap<Integer, TransmissionQueue> clientTransmissionFrameQueues = new ConcurrentHashMap<>();
                this.transmissionFrameQueueTree.put(assignedClientID, clientTransmissionFrameQueues);
                
                ACKSet clientAckQueue = new ACKSet();
                ackQueues.put(assignedClientID, clientAckQueue);
                
                clientReceiveSetup(assignedClientID, udpSocket, firstFrameNumber, clientTransmissionFrameQueues);

                barrier.await();

                //Send second reply
                ClientsAnnouncement announcement = new ClientsAnnouncement();
                announcement.clientIDs = ArrayUtils.toPrimitive(this.clientIDs.toArray(new Integer[0]));
                oout.writeObject(announcement);
                
                clientTransmissionSetup(assignedClientID, firstFrameNumber, udpSocket, clientTransmissionFrameQueues);

                //Wait for other handshakes to reach final step
                barrier.await();
                oout.writeObject(new SimulationStart());   

                //Continue with execution
                latch.countDown();
            }
        } 
        catch (IOException | ClassNotFoundException ioEx)
        {
            LOG.fatal("Exception at handshake with client");
            LOG.fatal(ioEx);
            server.interrupt();
        }
        catch (InterruptedException | BrokenBarrierException inEx)
        {
            //Interruptions come from failure in parallel handshake sessions, and signal termination
        }            
    }
    
    private void clientReceiveSetup(int clientID, DatagramSocket clientUDPSocket, int initialFrameNumber, ConcurrentMap<Integer, TransmissionQueue> transmissionFrameQueues)
    {
        ServerReceivingQueue receivingQueue = new ServerReceivingQueue(initialFrameNumber, clientID, executionSemaphore);
        this.receivingQueues.put(clientID, receivingQueue);
        ConcurrentHashMap<Integer,ReceivingQueue> receivingQueueWrapper = new ConcurrentHashMap<>();
        receivingQueueWrapper.put(clientID, receivingQueue);
        
        LOG.info("Receiver AckQueue("+clientID+"): " + ackQueues.get(clientID));
        
        LockstepReceiver receiver = LockstepReceiver.builder()
                .dgramSocket(clientUDPSocket)
                .coreThread(this)
                .receiverID(clientID)
                .receivingQueues(receivingQueueWrapper)
                .transmissionQueues(transmissionFrameQueues)
                .name("Receiver-from-"+clientID)
                .ackSet(ackQueues.get(clientID))
                .connectionTimeout(connectionTimeout)
                .build();
        
        receivers.put(clientID, receiver);
        receiver.start();
    }
    
    private void clientTransmissionSetup(int clientID, int firstFrameNumber, DatagramSocket udpSocket, Map<Integer, TransmissionQueue> clientTransmissionFrameQueues)
    {
        for(int hostID : clientIDs)
        {
            if(hostID != clientID)
            {
                TransmissionQueue transmissionFrameQueue = new TransmissionQueue(firstFrameNumber, hostID);
                clientTransmissionFrameQueues.put(hostID, transmissionFrameQueue);
            }
        }

        LOG.info("Transmitter AckQueue("+clientID+"): " + ackQueues.get(clientID));
        
        LockstepTransmitter transmitter = LockstepTransmitter.builder()
                .dgramSocket(udpSocket)
                .tickrate(tickrate)
                .keepAliveTimeout(0)
                .transmissionQueues(clientTransmissionFrameQueues)
                .name("Transmitter-to-"+clientID)
                .ackSet(ackQueues.get(clientID))
                .build();
        
        transmitters.put(clientID, transmitter);
        transmitter.start();        
    }
    
    private Map<Integer, FrameInput> collectFrameInputs()
    {        
        Map<Integer, FrameInput> nextCommands = new TreeMap<>();
        boolean foundFirstFrame = false;
        for(Entry<Integer, ServerReceivingQueue> serverQueueEntry : this.receivingQueues.entrySet())
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
    
    private void forwardFrameInputs(Map<Integer, FrameInput> nextFrameInputs)
    {
        //For each command
        for(Entry<Integer, FrameInput> frameEntry : nextFrameInputs.entrySet())
        {
            Integer senderID = frameEntry.getKey();
            FrameInput input = frameEntry.getValue();
            
            //For each client, take its tree of transmission queues
            for(Entry<Integer, Map<Integer, TransmissionQueue>> transmissionFrameQueueMapEntry : this.transmissionFrameQueueTree.entrySet())
            {
                Integer recipientID = transmissionFrameQueueMapEntry.getKey();
                
                //If the frameInput doesn't come from that client, forward the frameInput though the correct transmission queue
                if(!recipientID.equals(senderID))
                {
                    Map<Integer, TransmissionQueue> recipientTransmissionQueueMap = transmissionFrameQueueMapEntry.getValue();
                    TransmissionQueue transmissionFrameQueueFromSender = recipientTransmissionQueueMap.get(senderID);
                    transmissionFrameQueueFromSender.push(input);
                
                    if(input.getCommand() instanceof DisconnectionSignal)
                    {
                        if(receivingQueues.containsKey(senderID))
                            disconnectReceivingQueues(senderID);
                    }
                        
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
 
    /**
     * First step of a client disconnection.
     * The transmitting queues are removed as no other frame needs to be sent
     * to the disconnected client.
     * @param nodeID ID of the disconnected client
     */
    @Override
    public void disconnectTransmittingQueues(int nodeID)
    {
        transmissionFrameQueueTree.remove(nodeID);
        LOG.info("Disconnected transmission queues for " + nodeID);
    }
    
    /**
     * Second step of a client disconnection.
     * After the last frame has been forwarded, the receiving queue is cleaned.
     * @param nodeID ID of the disconnected client
     */    
    @Override
    void disconnectReceivingQueues(int nodeID)
    { 
       receivingQueues.remove(nodeID);
        LOG.info("Disconnected receiving queue for " + nodeID);
        
        clientsNumber--;
        
        LOG.info(""+clientsNumber+"remaining");
        if(clientsNumber == 1)
            this.interrupt();
    }

    /**
     * Forces the server to free its resources and stop.
     */
    @Override
    public void abort()
    {
        this.interrupt();
    }
}
