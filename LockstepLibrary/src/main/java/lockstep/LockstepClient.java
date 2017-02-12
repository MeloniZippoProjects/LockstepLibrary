/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lockstep;

import java.io.IOException;
import lockstep.messages.handshake.*;
import lockstep.CyclicCountDownLatch;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.log4j.Logger;

/**
 *
 * @author Raff
 * @param <Command>
 */
public abstract class LockstepClient<Command extends Serializable> implements Runnable
{
    int currentFrame;
    int interframeTime;
    int frameExecutionDistance;
    int hostID;
    Map<Integer, ExecutionFrameQueue> executionFrameQueues; 
    TransmissionFrameQueue transmissionFrameQueue;
    
    InetSocketAddress serverTCPAddress;
    
    static final int executionQueueBufferSize = 1024;
    
    ExecutorService executorService;
    
    LockstepReceiver receiver;
    LockstepTransmitter transmitter;
    
    private static final Logger LOG = Logger.getLogger(LockstepClient.class.getName());
        
    /**
     * Used for synchronization between server and executionFrameQueues
     */
    CyclicCountDownLatch inputLatch;
    private int clientsNumber;

    public LockstepClient(InetSocketAddress serverTCPAddress)
    {
        this.serverTCPAddress = serverTCPAddress;
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
     * @param f the frame input containing the command to execute
     */
    protected abstract void executeFrameInput(FrameInput f);

    /**
     * Provides the first commands to bootstart the simulation.
     * 
     * @return array of commands to bootstart the simulation
     */
    protected abstract Command[] fillCommands();
        
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
                currentFrame++;
                Thread.sleep(interframeTime);
            }
            catch(InterruptedException e)
            {
                e.printStackTrace();
            }           
        }
    }

    private void handshake()
    {
        try(
            Socket tcpSocket = new Socket(serverTCPAddress.getAddress(), serverTCPAddress.getPort());
            ObjectOutputStream oout = new ObjectOutputStream(tcpSocket.getOutputStream());
            ObjectInputStream oin = new ObjectInputStream(tcpSocket.getInputStream());
        )
        {
            //Bind own UDP socket
            DatagramSocket udpSocket = new DatagramSocket();
            udpSocket.bind(null);
            LOG.info("Opened connection on " + udpSocket.getLocalAddress().getHostAddress() + ":" + udpSocket.getLocalPort());
            
            LOG.info("Start handshake with server");
            
            //Send hello to server, with the bound UDP port
            oout.write(new ClientHello().clientUDPPort = udpSocket.getLocalPort());
            LOG.info("Sent ClientHello message");
            
            //Receive and process first server reply
            ServerHelloReply helloReply = (ServerHelloReply) oin.readObject();
            LOG.info("Received reply from server");
            this.hostID = helloReply.assignedHostID;
            this.currentFrame = helloReply.firstFrameNumber;
            
            this.executionFrameQueues = new ConcurrentHashMap<>();
            this.executionFrameQueues.put(hostID, new ExecutionFrameQueue(executionQueueBufferSize, helloReply.firstFrameNumber, hostID));
            
            //Network setup
            LOG.info("Setting up network threads and stub frames");

            InetSocketAddress serverUDPAddress = new InetSocketAddress(serverTCPAddress.getAddress(), helloReply.serverUDPPort);
            udpSocket.connect(serverUDPAddress);

            Map<Integer, ExecutionFrameQueue> receivingExecutionQueues = new ConcurrentHashMap<>();
            transmissionFrameQueue = new TransmissionFrameQueue(helloReply.firstFrameNumber);
            HashMap<Integer,TransmissionFrameQueue> transmissionQueueWrapper = new HashMap<>();
            transmissionQueueWrapper.put(hostID, transmissionFrameQueue);
            
            receiver = new LockstepReceiver(udpSocket, receivingExecutionQueues, transmissionQueueWrapper);
            transmitter = new LockstepTransmitter(udpSocket, transmissionQueueWrapper);

            fillFrameBuffer(fillCommands());
            executorService = Executors.newFixedThreadPool(2);

            //Receive and process second server reply
            ClientsAnnouncement clientsAnnouncement = (ClientsAnnouncement) oin.readObject();
            LOG.info("Received list of clients from server");
            
            clientsNumber = clientsAnnouncement.hostIDs.length;
            inputLatch = new CyclicCountDownLatch(clientsNumber);

            for(int clientID : clientsAnnouncement.hostIDs)
            {
                ExecutionFrameQueue executionFrameQueue = new ExecutionFrameQueue(executionQueueBufferSize, helloReply.firstFrameNumber, clientID, inputLatch);
                executionFrameQueues.put(clientID, executionFrameQueue);
                receivingExecutionQueues.put(clientID, executionFrameQueue);
            }

            //Wait for simulation start signal to proceed executing
            SimulationStart start = (SimulationStart) oin.readObject();
            LOG.info("Simulation started");
        }
        catch(ClassNotFoundException e)
        {
            LOG.fatal("The received Object class cannot be found");
            System.exit(1);
        }
        catch(IOException e)
        {
            LOG.error("IO error");
        }
    }
    
    private void fillFrameBuffer(Command[] fillCommands)
    {
        for(Command cmd : fillCommands)
        {
            executionFrameQueues.get(this.hostID).push(new FrameInput(currentFrame, cmd));
            transmissionFrameQueue.push(new FrameInput(currentFrame, cmd));
        }
        this.frameExecutionDistance = fillCommands.length;
    }
    
    private void readUserInput()
    {
        Command cmd = readInput();
        executionFrameQueues.get(this.hostID).push(new FrameInput(currentFrame + frameExecutionDistance, cmd));
        transmissionFrameQueue.push(new FrameInput(currentFrame + frameExecutionDistance, cmd));
    }
    
    private void executeInputs() throws InterruptedException
    {
        if(executionQueuesHeadsAvailability.containsValue(Boolean.FALSE))
        {
            suspendSimulation();
            for(Integer key : executionQueuesHeadsAvailability.keySet())
            {
                if(key != this.hostID)
                {
                    Boolean nextQueueHeadAvailability = executionQueuesHeadsAvailability.get(key);
                    synchronized(nextQueueHeadAvailability)
                    {
                        while(nextQueueHeadAvailability == Boolean.FALSE)
                        {
                            nextQueueHeadAvailability.wait();
                        }
                    }
                }
            }
            resumeSimulation();
        }
                
        FrameInput[] inputs = collectFrameInputs();
        for(FrameInput input : inputs)
            executeFrameInput(input);
    }

    private FrameInput[] collectFrameInputs()
    {
        FrameInput[] inputs = new FrameInput[this.executionFrameQueues.size()];
        int idx = 0;
        for(ExecutionFrameQueue frameQueue : this.executionFrameQueues.values())
        {
            inputs[idx] = frameQueue.pop();
            ++idx;
        }        
        return inputs;
    }
}
