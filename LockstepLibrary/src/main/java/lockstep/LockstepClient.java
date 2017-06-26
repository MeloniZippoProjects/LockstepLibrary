/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lockstep;

import java.io.IOException;
import lockstep.messages.handshake.*;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import lockstep.messages.simulation.DisconnectionSignal;
import lockstep.messages.simulation.LockstepCommand;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * Main thread for a client.
 * It handles the handshake with the server, setting up data structures and
 * child threads.
 * Then it loops obtaining inputs from all other clients and, only if in order,
 * sends them to the application for execution.
 * The thread blocks on a semaphore, suspending the application, each time the
 * in order inputs are not ready.
 */
public class LockstepClient extends LockstepCoreThread
{
    int framerate;
    int fillTimeout;
    
    int currentExecutionFrame;
    int currentUserFrame;
    int frameExecutionDistance = 0;
    int maxExecutionDistance;
    int localClientID;
    
    ConcurrentSkipListMap<Integer, ClientReceivingQueue> executionFrameQueues; 
    TransmissionQueue transmissionFrameQueue;
    
    InetSocketAddress serverTCPAddress;
    
    LockstepReceiver receiver;
    LockstepTransmitter transmitter;
    
    DatagramSocket udpSocket;
    
    Semaphore executionSemaphore;
    int clientsNumber;
    final int tickrate;
    final int connectionTimeout;
    final LockstepApplication lockstepApplication;

    static final Logger LOG = LogManager.getLogger(LockstepClient.class);
    
    public LockstepClient(InetSocketAddress serverTCPAddress, int framerate, 
            int tickrate, int fillTimeout, int maxExecutionDistance,
            int connectionTimeout, LockstepApplication lockstepApplication)
    {
        if(serverTCPAddress.isUnresolved()) 
            throw new IllegalArgumentException("Server hostname is unresolved");
        else
            this.serverTCPAddress = serverTCPAddress;
        
        if(framerate <= 0)
            throw new IllegalArgumentException("Framerate must be an integer greater than 0");
        else
            this.framerate = framerate;
        
        if(tickrate <= 0)
            throw new IllegalArgumentException("Tickrate must be an integer greater than 0");
        else
            this.tickrate = tickrate;
        
        if(fillTimeout <= 0)
            throw new IllegalArgumentException("Fill timeout must be an integer greater than 0");
        else
            this.fillTimeout = fillTimeout;
        
        if(maxExecutionDistance <= 0)
            throw new IllegalArgumentException("Max execution distance must be an integer greater than 0");
        else
            this.maxExecutionDistance = maxExecutionDistance;
        
        if(connectionTimeout < 0)
            throw new IllegalArgumentException("Connection timeout timeout must greater or equal than zero");
        else
            this.connectionTimeout = connectionTimeout;
        
        if(lockstepApplication == null)
            throw new NullPointerException("LockstepApplication cannot be null");
        else
            this.lockstepApplication = lockstepApplication;
    }

    public static class Builder {

        private int framerate;
        private int fillTimeout;
        private int maxExecutionDistance;
        private InetSocketAddress serverTCPAddress;
        private int tickrate;
        private int connectionTimeout;
        private LockstepApplication lockstepApplication;

        private Builder() {
        }

        public Builder framerate(final int value) {
            this.framerate = value;
            return this;
        }

        public Builder fillTimeout(final int value) {
            this.fillTimeout = value;
            return this;
        }
        
        public Builder maxExecutionDistance(final int value) {
            this.maxExecutionDistance = value;
            return this;
        }
        
        public Builder connectionTimeout(final int value) {
            this.connectionTimeout = value;
            return this;
        }

        public Builder serverTCPAddress(final InetSocketAddress value) {
            this.serverTCPAddress = value;
            return this;
        }

        public Builder tickrate(final int value) {
            this.tickrate = value;
            return this;
        }

        public Builder lockstepApplication(final LockstepApplication value) {
            this.lockstepApplication = value;
            return this;
        }
        
        public LockstepClient build() {
            return new lockstep.LockstepClient(serverTCPAddress, framerate, tickrate, fillTimeout, maxExecutionDistance, connectionTimeout, lockstepApplication);
        }
    }

    public static LockstepClient.Builder builder() {
        return new LockstepClient.Builder();
    }

        
    @Override
    public void run()
    {
        try{
            clientHandshakeProtocol();
        } catch( ClassNotFoundException | IOException ex)
        {
            LOG.fatal("Handshake failed");
            LOG.fatal(ex);
            lockstepApplication.signalHandshakeFailure();
            return;
        }
        
        while(true)
        {
            try
            {
                if(Thread.interrupted())
                    throw new InterruptedException();
                
                readUserInput();
                executeInputs();
                currentExecutionFrame++;
                Thread.sleep(1000/framerate);
            }
            catch(InterruptedException intEx)
            {
                networkShutdown();
                return;
            }           
        }
    }

    private void clientHandshakeProtocol() throws ClassNotFoundException, IOException
    {
        LOG.info("Starting handshake");

        Socket tcpSocket = new Socket(serverTCPAddress.getAddress(), serverTCPAddress.getPort());
        ObjectOutputStream oout = new ObjectOutputStream(tcpSocket.getOutputStream());                        
        
        oout.flush();
        ObjectInputStream oin = new ObjectInputStream(tcpSocket.getInputStream());
            
        //Bind own UDP socket
        udpSocket = new DatagramSocket();
        LOG.info("Opened connection on " + udpSocket.getLocalAddress().getHostAddress() + ":" + udpSocket.getLocalPort());

        //Send hello to server, with the bound UDP port
        LOG.info("Sending ClientHello message");
        ClientHello clientHello = new ClientHello();
        clientHello.clientUDPPort = udpSocket.getLocalPort();
        oout.writeObject(clientHello);

        //Receive and process first server reply
        LOG.info("Waiting for helloReply from server");
        ServerHelloReply helloReply = (ServerHelloReply) oin.readObject();
        localClientID = helloReply.assignedClientID;
        LOG.info("ID assigned = " + localClientID);
        currentExecutionFrame = helloReply.firstFrameNumber;
        currentUserFrame = helloReply.firstFrameNumber;

        clientsNumber = helloReply.clientsNumber;
        executionSemaphore = new Semaphore(0);
        executionFrameQueues = new ConcurrentSkipListMap<>();
        executionFrameQueues.put(localClientID, new ClientReceivingQueue(helloReply.firstFrameNumber, localClientID, executionSemaphore));

        //Network setup
        LOG.info("Setting up network threads and stub frames");

        InetSocketAddress serverUDPAddress = new InetSocketAddress(serverTCPAddress.getAddress(), helloReply.serverUDPPort);
        udpSocket.connect(serverUDPAddress);

        ConcurrentHashMap<Integer, ReceivingQueue> receivingExecutionQueues = new ConcurrentHashMap<>();
        transmissionFrameQueue = new TransmissionQueue(helloReply.firstFrameNumber, localClientID);
        ConcurrentHashMap<Integer,TransmissionQueue> transmissionQueueWrapper = new ConcurrentHashMap<>();
        transmissionQueueWrapper.put(localClientID, transmissionFrameQueue);

        ACKSet ackSet = new ACKSet();
        
        receiver = LockstepReceiver.builder()
                .dgramSocket(udpSocket)
                .coreThread(this)
                .receivingQueues(receivingExecutionQueues)
                .transmissionQueues(transmissionQueueWrapper)
                .name("Receiver-to-"+localClientID)
                .receiverID(LockstepReceiver.RECEIVER_FROM_SERVER_ID)
                .ackSet(ackSet)
                .connectionTimeout(connectionTimeout)
                .build();        

        transmitter = LockstepTransmitter.builder()
                .dgramSocket(udpSocket)
                .tickrate(tickrate)
                .keepAliveTimeout(1000)
                .transmissionQueues(transmissionQueueWrapper)
                .name("Transmitter-from-"+localClientID)
                .ackSet(ackSet)
                .build();
                
        insertFillCommands(lockstepApplication.bootstrapCommands());

        transmitter.start();

        //Receive and process second server reply
        LOG.info("Waiting for list of clients from server");
        ClientsAnnouncement clientsAnnouncement = (ClientsAnnouncement) oin.readObject();

        for(int clientID : clientsAnnouncement.clientIDs)
        {
            if(clientID != localClientID)
            {
                ClientReceivingQueue executionFrameQueue = new ClientReceivingQueue(helloReply.firstFrameNumber, clientID, executionSemaphore);
                executionFrameQueues.put(clientID, executionFrameQueue);
                receivingExecutionQueues.put(clientID, executionFrameQueue);
            }
        }

        receiver.start();

        //Wait for simulation start signal to proceed executing
        LOG.info("Waiting for simulation start signal");
        SimulationStart start = (SimulationStart) oin.readObject();
        LOG.info("Simulation started");
    }
    
    private void insertFillCommands(LockstepCommand[] fillCommands)
    {
        for (LockstepCommand cmd : fillCommands)
        {
            if(frameExecutionDistance < maxExecutionDistance)
            {
                FrameInput newFrame = new FrameInput(currentUserFrame++, cmd);
                executionFrameQueues.get(this.localClientID).push(newFrame);
                if(transmissionFrameQueue != null)
                    transmissionFrameQueue.push(newFrame);
                frameExecutionDistance++;
            }
            else
                return;
        }
    }
    
    private void readUserInput()
    {
        LockstepCommand cmd = lockstepApplication.readInput();
        FrameInput newFrame = new FrameInput(currentUserFrame++, cmd);
        executionFrameQueues.get(this.localClientID).push(newFrame);
        if(transmissionFrameQueue != null)
            transmissionFrameQueue.push(newFrame);
    }
    
    private void executeInputs() throws InterruptedException
    {
        if(!executionSemaphore.tryAcquire(clientsNumber))
        {
            lockstepApplication.suspendSimulation();

            if(fillTimeout > 0 && frameExecutionDistance < maxExecutionDistance)
            {
                if(!executionSemaphore.tryAcquire(clientsNumber, fillTimeout, TimeUnit.MILLISECONDS))
                {
                    insertFillCommands(lockstepApplication.fillCommands());
                    executionSemaphore.acquire(clientsNumber);
                }
            }
            else
                executionSemaphore.acquire(clientsNumber);

            lockstepApplication.resumeSimulation();
        }
        
        TreeMap<Integer, LockstepCommand> commands = collectCommands();
        for(Entry<Integer, LockstepCommand> commandEntry : commands.entrySet())
        {
            LockstepCommand command = commandEntry.getValue();
            int senderID = commandEntry.getKey();
            
            if(command instanceof DisconnectionSignal)
                disconnectReceivingQueues(senderID);
            else
                lockstepApplication.executeCommand(command);
        }
    }

    private TreeMap<Integer, LockstepCommand> collectCommands()
    {        
        TreeMap<Integer, LockstepCommand> commands = new TreeMap<>();
        
        for(Entry<Integer, ClientReceivingQueue> frameQueueEntry : this.executionFrameQueues.entrySet())
        {
            Integer senderID = frameQueueEntry.getKey();
            ClientReceivingQueue frameQueue = frameQueueEntry.getValue();
           
            FrameInput input = frameQueue.pop();
            if(input != null)
                commands.put(senderID, input.getCommand());
        }
                
        return commands;
    }
    
    @Override
    public void disconnectTransmittingQueues(int nodeID)
    {
        LOG.info("Disconnecting node: " + nodeID);
        if(nodeID == LockstepReceiver.RECEIVER_FROM_SERVER_ID)
        {
            transmissionFrameQueue = null;
            LOG.info("Disconnected trasmission queue");
        }
    }

    @Override
    void disconnectReceivingQueues(int nodeID)
    {
        executionFrameQueues.remove(nodeID);
        clientsNumber--;
        LOG.info("Disconnected receiving queue for " + nodeID);
        
        lockstepApplication.signalDisconnection(clientsNumber);
    }
    
    void networkShutdown()
    {
        transmitter.interrupt();
        
        try
        {
            receiver.join();
            transmitter.join();
        }
        catch(InterruptedException intEx)
        {
           LOG.fatal("Interrupted during termination, child threads may be still running");
           LOG.fatal(intEx);
        }
    }
    
    @Override
    public void abort()
    {
        this.interrupt();
    }
}
