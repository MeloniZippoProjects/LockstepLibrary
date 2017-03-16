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
import java.io.Serializable;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;

/**
 *
 * @author Raff
 * @param <Command>
 */
public abstract class LockstepClient<Command extends Serializable> implements Runnable
{
    int framerate;
    int fillTimeout;
    
    int currentExecutionFrame;
    int currentUserFrame;
    int frameExecutionDistance;
    int hostID;
    Map<Integer, ExecutionFrameQueue<Command>> executionFrameQueues; 
    TransmissionFrameQueue<Command> transmissionFrameQueue;
    
    InetSocketAddress serverTCPAddress;
        
    ExecutorService executorService;
    
    LockstepReceiver receiver;
    LockstepTransmitter transmitter;
    
    private int UDPPort = 10240;
    
    private static final Logger LOG = Logger.getLogger(LockstepClient.class.getName());
        
    /**
     * Used for synchronization between server and executionFrameQueues
     */
    CyclicCountDownLatch cyclicExecutionLatch;
    private int clientsNumber;
    private final int tickrate;
    
    public LockstepClient(InetSocketAddress serverTCPAddress, int framerate, int tickrate, int fillTimeout)
    {
        this.serverTCPAddress = serverTCPAddress;
        this.framerate = framerate;
        this.tickrate = tickrate;
        this.fillTimeout = fillTimeout;
    }

    /**
     * Must read input from user, and return a Command object to be executed.
     * If there is no input in a frame a Command object must still be returned,
     * possibly representing the lack of an user input in the semantic of the application
     * 
     * @return the Command object collected in the current frame
     */
    protected abstract Command readInput();


    /**
     * Must suspend the simulation execution due to a synchronization issue
     */
    protected abstract void suspendSimulation();
    
    /**
     * Must resume the simulation execution, as input are now synchronized
     */
    protected abstract void resumeSimulation();

    /**
     * Must get the command contained in the frame input and execute it
     * 
     * @param c the command to execute
     */
    protected abstract void executeCommand(Command c);

    /**
     * Provides void commands to resume from a deadlock situation.
     * Their number should be dimensioned to take less time than the user to
     * react from the simulation being resumed
     * 
     * @return array of commands to bootstart the simulation
     */
    protected abstract Command[] fillCommands();
        
    /**
     * Provides the first commands to bootstrap the simulation.
     * 
     * @return 
     */
    protected abstract Command[] bootstrapCommands();
        
    @Override
    public void run()
    {
        handshake();
        
        while(true)
        {
            try
            {
                readUserInput();
                executeInputs();
                currentExecutionFrame++;
                Thread.sleep(1000/framerate);
            }
            catch(InterruptedException e)
            {
                e.printStackTrace();
            }           
        }
    }

    private void handshake()
    {
        LOG.debug("Start of handshake");
        try(
            Socket tcpSocket = new Socket(serverTCPAddress.getAddress(), serverTCPAddress.getPort());
            ObjectOutputStream oout = new ObjectOutputStream(tcpSocket.getOutputStream());                        
            //ObjectInputStream oin = new ObjectInputStream(tcpSocket.getInputStream());
        )
        {
            oout.flush();
            LOG.debug("oout flushed");
            try(ObjectInputStream oin = new ObjectInputStream(tcpSocket.getInputStream()))
            {
                //Bind own UDP socket
                DatagramSocket udpSocket = new DatagramSocket();
                LOG.info("Opened connection on " + udpSocket.getLocalAddress().getHostAddress() + ":" + udpSocket.getLocalPort());

                //Send hello to server, with the bound UDP port
                LOG.info("Sending ClientHello message");
                ClientHello clientHello = new ClientHello();
                clientHello.clientUDPPort = udpSocket.getLocalPort();
                oout.writeObject(clientHello);

                //Receive and process first server reply
                LOG.info("Waiting for helloReply from server");
                ServerHelloReply helloReply = (ServerHelloReply) oin.readObject();
                this.hostID = helloReply.assignedHostID;
                LOG.info("ID assigned = " + hostID);
                this.currentExecutionFrame = helloReply.firstFrameNumber;
                this.currentUserFrame = helloReply.firstFrameNumber;

                clientsNumber = helloReply.clientsNumber;
                cyclicExecutionLatch = new CyclicCountDownLatch(clientsNumber);
                this.executionFrameQueues = new ConcurrentHashMap<>();
                this.executionFrameQueues.put(hostID, new ExecutionFrameQueue<>(helloReply.firstFrameNumber, hostID, cyclicExecutionLatch));

                //Network setup
                LOG.info("Setting up network threads and stub frames");

                InetSocketAddress serverUDPAddress = new InetSocketAddress(serverTCPAddress.getAddress(), helloReply.serverUDPPort);
                udpSocket.connect(serverUDPAddress);

                Map<Integer, ExecutionFrameQueue> receivingExecutionQueues = new ConcurrentHashMap<>();
                Semaphore transmissionSemaphore = new Semaphore(0);
                transmissionFrameQueue = new TransmissionFrameQueue(helloReply.firstFrameNumber, transmissionSemaphore, hostID);
                HashMap<Integer,TransmissionFrameQueue> transmissionQueueWrapper = new HashMap<>();
                transmissionQueueWrapper.put(hostID, transmissionFrameQueue);

                receiver = new LockstepReceiver(udpSocket, receivingExecutionQueues, transmissionQueueWrapper, "Receiver-to-"+hostID);
                transmitter = new LockstepTransmitter(udpSocket, tickrate, transmissionQueueWrapper, transmissionSemaphore, "Transmitter-from-"+hostID);

                insertBootstrapCommands(bootstrapCommands());
                                
                executorService = Executors.newFixedThreadPool(2);

                executorService.submit(transmitter);

                //Receive and process second server reply
                LOG.info("Waiting for list of clients from server");
                ClientsAnnouncement clientsAnnouncement = (ClientsAnnouncement) oin.readObject();

                for(int clientID : clientsAnnouncement.hostIDs)
                {
                    if(clientID != hostID)
                    {
                        ExecutionFrameQueue executionFrameQueue = new ExecutionFrameQueue(helloReply.firstFrameNumber, clientID, cyclicExecutionLatch);
                        executionFrameQueues.put(clientID, executionFrameQueue);
                        receivingExecutionQueues.put(clientID, executionFrameQueue);
                    }
                }
                
                executorService.submit(receiver);
                
                //Wait for simulation start signal to proceed executing
                LOG.info("Waiting for simulation start signal");
                SimulationStart start = (SimulationStart) oin.readObject();
                LOG.info("Simulation started");
            }
        }
        catch(ClassNotFoundException e)
        {
            LOG.fatal("The received Object class cannot be found");
            System.exit(1);
        }
        catch(IOException e)
        {
            LOG.fatal("IO error");
            LOG.fatal(e);
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    private void insertBootstrapCommands(Command[] bootstrapCommands)
    {
        insertFillCommands(bootstrapCommands);
        this.frameExecutionDistance = bootstrapCommands.length;
    }
    
    private void insertFillCommands(Command[] fillCommands)
    {
        for (int i = 0; i < fillCommands.length; i++)
        {
            Command cmd = fillCommands[i];
            executionFrameQueues.get(this.hostID).push(new FrameInput(currentUserFrame++, cmd));
            transmissionFrameQueue.push(cmd);
        }
    }
    
    private void readUserInput()
    {
        Command cmd = readInput();
        executionFrameQueues.get(this.hostID).push(new FrameInput(currentUserFrame++, cmd));
        transmissionFrameQueue.push(cmd);
    }
    
    private void executeInputs() throws InterruptedException
    {
        for(ExecutionFrameQueue exQ : executionFrameQueues.values())
                LOG.debug(exQ);

        LOG.debug(transmissionFrameQueue);
        
        if(cyclicExecutionLatch.getCount() > 0)
        {
            suspendSimulation();
            
            debugSimulation();
            
            if( !cyclicExecutionLatch.await(fillTimeout, TimeUnit.MILLISECONDS))
            {
//                LOG.debug("Inserting fillers to escape deadlock");
                //insertFillCommands(fillCommands());
                cyclicExecutionLatch.await();
            }
            else
            {
                cyclicExecutionLatch.reset();
            }
            
            resumeSimulation();
        }
        else
            cyclicExecutionLatch.reset();
        
        ArrayList<Command> commands = collectCommands();
        for(Command command : commands)
            executeCommand(command);
    }

    private ArrayList<Command> collectCommands()
    {
        ArrayList<Command> commands = new ArrayList<>();
        for(ExecutionFrameQueue<Command> frameQueue : this.executionFrameQueues.values())
            commands.add(frameQueue.pop());
        
        return commands;
    }

    private void debugSimulation()
    {
        System.out.println("---------------------------------------------");
        System.out.println("SIMULAZIONE SOSPESA, STAMPA STATO SIMULAZIONE");
        
        System.out.println("Stato execution frame queues");
        for(ExecutionFrameQueue<Command> exeFrameQueue : this.executionFrameQueues.values())
            System.out.println(exeFrameQueue);
        
        System.out.println("Stato transmission frame queue");
        System.out.println(transmissionFrameQueue);
        
        System.out.println("Stato numero frame");
        System.out.println("Current User Frame: " + currentUserFrame);
        System.out.println("Current Execution Frame: " + currentExecutionFrame);
        System.out.println("FrameExecutionDistance: " + frameExecutionDistance);
    }
}
