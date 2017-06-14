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
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.Semaphore;
import lockstep.messages.simulation.DisconnectionSignal;
import lockstep.messages.simulation.LockstepCommand;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class LockstepClient extends LockstepCoreThread
{
    int framerate;
    int fillTimeout;
    
    int currentExecutionFrame;
    int currentUserFrame;
    int frameExecutionDistance;
    int hostID;
    ConcurrentSkipListMap<Integer, ClientReceivingQueue> executionFrameQueues; 
    TransmissionQueue transmissionFrameQueue;
    
    InetSocketAddress serverTCPAddress;
    
    LockstepReceiver receiver;
    LockstepTransmitter transmitter;
    
    private DatagramSocket udpSocket;
    
    private static final Logger LOG = LogManager.getLogger(LockstepClient.class);
        
    /**
     * Used for synchronization between server and executionFrameQueues
     */
    Semaphore executionSemaphore;
    private int clientsNumber;
    private final int tickrate;
    private final LockstepApplication lockstepApplication;
    
    public LockstepClient(InetSocketAddress serverTCPAddress, int framerate, int tickrate, int fillTimeout, LockstepApplication lockstepApplication)
    {
        this.serverTCPAddress = serverTCPAddress;
        this.framerate = framerate;
        this.tickrate = tickrate;
        this.fillTimeout = fillTimeout;
        this.lockstepApplication = lockstepApplication;
    }

        
    @Override
    public void run()
    {
        try{
            handshake();
        } catch( ClassNotFoundException | IOException ex)
        {
            LOG.fatal("Handshake failed");
            LOG.fatal(ex);
            lockstepApplication.signalHandshakeFailure();
            return;
        }
        
        while(true)
        {
            //check if thread was interrupted
            
            try
            {
                if(Thread.interrupted())
                    throw new InterruptedException();
                
                readUserInput();
                executeInputs();
                currentExecutionFrame++;
                Thread.sleep(1000/framerate);
            }
            catch(InterruptedException e)
            {
                networkShutdown();
                return;
            }           
        }
    }

    private void handshake() throws ClassNotFoundException, IOException
    {
        LOG.debug("Start of handshake");

        Socket tcpSocket = new Socket(serverTCPAddress.getAddress(), serverTCPAddress.getPort());
        ObjectOutputStream oout = new ObjectOutputStream(tcpSocket.getOutputStream());                        
        
        oout.flush();
        LOG.debug("oout flushed");
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
        hostID = helloReply.assignedHostID;
        LOG.info("ID assigned = " + hostID);
        currentExecutionFrame = helloReply.firstFrameNumber;
        currentUserFrame = helloReply.firstFrameNumber;

        clientsNumber = helloReply.clientsNumber;
        executionSemaphore = new Semaphore(0);
        executionFrameQueues = new ConcurrentSkipListMap<>();
        executionFrameQueues.put(hostID, new ClientReceivingQueue(helloReply.firstFrameNumber, hostID, executionSemaphore));

        //Network setup
        LOG.info("Setting up network threads and stub frames");

        InetSocketAddress serverUDPAddress = new InetSocketAddress(serverTCPAddress.getAddress(), helloReply.serverUDPPort);
        udpSocket.connect(serverUDPAddress);

        udpSocket.setSoTimeout(5000);

        Map<Integer, ReceivingQueue> receivingExecutionQueues = new ConcurrentHashMap<>();
        transmissionFrameQueue = new TransmissionQueue(helloReply.firstFrameNumber, hostID);
        HashMap<Integer,TransmissionQueue> transmissionQueueWrapper = new HashMap<>();
        transmissionQueueWrapper.put(hostID, transmissionFrameQueue);

        ACKQueue ackQueue = new ACKQueue();
        receiver = new LockstepReceiver(udpSocket, this, receivingExecutionQueues, transmissionQueueWrapper, "Receiver-to-"+hostID, LockstepReceiver.RECEIVER_FROM_SERVER_ID, ackQueue);
        transmitter = new LockstepTransmitter(udpSocket, tickrate, 1000, transmissionQueueWrapper, "Transmitter-from-"+hostID, ackQueue);

        insertBootstrapCommands(lockstepApplication.bootstrapCommands());

        transmitter.start();

        //Receive and process second server reply
        LOG.info("Waiting for list of clients from server");
        ClientsAnnouncement clientsAnnouncement = (ClientsAnnouncement) oin.readObject();

        for(int clientID : clientsAnnouncement.hostIDs)
        {
            if(clientID != hostID)
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
    
    private void insertBootstrapCommands(LockstepCommand[] bootstrapCommands)
    {
        insertFillCommands(bootstrapCommands);
        this.frameExecutionDistance = bootstrapCommands.length;
    }
    
    private void insertFillCommands(LockstepCommand[] fillCommands)
    {
        for (LockstepCommand cmd : fillCommands)
        {
            FrameInput newFrame = new FrameInput(currentUserFrame++, cmd);
            executionFrameQueues.get(this.hostID).push(newFrame);
            if(transmissionFrameQueue != null)
                transmissionFrameQueue.push(newFrame);
        }
    }
    
    private void readUserInput()
    {
        LockstepCommand cmd = lockstepApplication.readInput();
        FrameInput newFrame = new FrameInput(currentUserFrame++, cmd);
        executionFrameQueues.get(this.hostID).push(newFrame);
        if(transmissionFrameQueue != null)
            transmissionFrameQueue.push(newFrame);
    }
    
    private void executeInputs() throws InterruptedException
    {
        
        for(ClientReceivingQueue exQ : executionFrameQueues.values())
                LOG.debug(exQ);

        LOG.debug(transmissionFrameQueue);
        
        if(!executionSemaphore.tryAcquire(clientsNumber))
        {
            lockstepApplication.suspendSimulation();
            executionSemaphore.acquire(clientsNumber);
            lockstepApplication.resumeSimulation();
        }
        
        HashMap<Integer, LockstepCommand> commands = collectCommands();
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

    private HashMap<Integer, LockstepCommand> collectCommands()
    {        
        HashMap<Integer, LockstepCommand> commands = new HashMap<>();
        
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

    private void debugSimulation()
    {
        System.out.println("---------------------------------------------");
        System.out.println("SIMULAZIONE SOSPESA, STAMPA STATO SIMULAZIONE");
        
        System.out.println("Stato execution frame queues");
        for(ClientReceivingQueue exeFrameQueue : this.executionFrameQueues.values())
            System.out.println(exeFrameQueue);
        
        System.out.println("Stato transmission frame queue");
        System.out.println(transmissionFrameQueue);
        
        System.out.println("Stato numero frame");
        System.out.println("Current User Frame: " + currentUserFrame);
        System.out.println("Current Execution Frame: " + currentExecutionFrame);
        System.out.println("FrameExecutionDistance: " + frameExecutionDistance);
    }

    @Override
    public void disconnectTransmittingQueues(int nodeID)
    {
        if(nodeID == LockstepReceiver.RECEIVER_FROM_SERVER_ID)
            transmissionFrameQueue = null;
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
        receiver.interrupt();
        transmitter.interrupt();
        
        try
        {
            receiver.join();
            transmitter.join();
        }
        catch(InterruptedException intEx)
        {
           //you should not get interrupted here:
           //what to do? retry joining or just ignore?
           LOG.fatal("Interrupted during termination!!");
           LOG.fatal(intEx);
        }
        
        udpSocket.close();
    }
    
    @Override
    public void abort()
    {
        this.interrupt();
    }
}
