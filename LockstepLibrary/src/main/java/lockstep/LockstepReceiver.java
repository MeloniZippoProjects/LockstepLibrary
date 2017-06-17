/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lockstep;

import lockstep.messages.simulation.InputMessageArray;
import lockstep.messages.simulation.InputMessage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import lockstep.messages.simulation.DisconnectionSignal;
import lockstep.messages.simulation.FrameACK;
import lockstep.messages.simulation.KeepAlive;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class LockstepReceiver extends Thread
{
    public static final int RECEIVER_FROM_SERVER_ID = 0;
    
    volatile DatagramSocket dgramSocket;
    volatile Map<Integer, ReceivingQueue> receivingQueues;
    volatile Map<Integer, TransmissionQueue> transmissionQueues;
    volatile ACKQueue ackQueue;
    static final int MAX_PAYLOAD_LENGTH = 300;
    
    private static final Logger LOG = LogManager.getLogger(LockstepReceiver.class);
    
    private final LockstepCoreThread coreThread;
    
    private final String name;
    
    int receiverID;
        
    public LockstepReceiver(DatagramSocket socket, LockstepCoreThread coreThread, 
            Map<Integer, ReceivingQueue> receivingQueues, 
            Map<Integer, TransmissionQueue> transmissionQueues, 
            String name, int ownID, ACKQueue ackQueue)
    {
        
        if(socket.isClosed())
            throw new IllegalArgumentException("Socket is closed");
        else
            this.dgramSocket = socket;
        
        if(coreThread == null)
            throw new IllegalArgumentException("Core Thread cannot be null");
        else
            this.coreThread = coreThread;
        
        if(receivingQueues == null)
            throw new IllegalArgumentException("Receiving Queues Map cannot be null");
        else
            this.receivingQueues = receivingQueues;
        
        if(transmissionQueues == null)
            throw new IllegalArgumentException("Transmission Queues Map cannot be null");
        else
            this.transmissionQueues = transmissionQueues;
        
        if(ownID < 0)
            throw new IllegalArgumentException("Receiver id cannot be negative");
        else
            this.receiverID = ownID;
        
        if(name != null)
            this.name = name;
        else
            this.name = "Receiver-"+ownID;
        
        if(ackQueue == null)
            throw new IllegalArgumentException("Ack Queue cannot be null");
        else
            this.ackQueue = ackQueue;
    }

    public static class Builder {

        private DatagramSocket dgramSocket;
        private Map<Integer,ReceivingQueue> receivingQueues;
        private Map<Integer,TransmissionQueue> transmissionFrameQueues;
        private ACKQueue ackQueue;
        private LockstepCoreThread coreThread;
        private String name;
        private int receiverID;

        private Builder() {
        }

        public Builder dgramSocket(final DatagramSocket value) {
            this.dgramSocket = value;
            return this;
        }

        public Builder receivingQueues(final Map<Integer,ReceivingQueue> value) {
            this.receivingQueues = value;
            return this;
        }

        public Builder transmissionQueues(final Map<Integer,TransmissionQueue> value) {
            this.transmissionFrameQueues = value;
            return this;
        }

        public Builder ackQueue(final ACKQueue value) {
            this.ackQueue = value;
            return this;
        }

        public Builder coreThread(final LockstepCoreThread value) {
            this.coreThread = value;
            return this;
        }

        public Builder name(final String value) {
            this.name = value;
            return this;
        }

        public Builder receiverID(final int value) {
            this.receiverID = value;
            return this;
        }

        public LockstepReceiver build() {
            return new lockstep.LockstepReceiver(dgramSocket, receivingQueues, 
                    transmissionFrameQueues, ackQueue, 
                    coreThread, name, receiverID);
        }
    }

    public static LockstepReceiver.Builder builder() {
        return new LockstepReceiver.Builder();
    }

    private LockstepReceiver(final DatagramSocket dgramSocket, final Map<Integer, ReceivingQueue> receivingQueues, final Map<Integer, TransmissionQueue> transmissionFrameQueues, final ACKQueue ackQueue, final LockstepCoreThread coreThread, final String name, final int receiverID) {
        this.dgramSocket = dgramSocket;
        this.receivingQueues = receivingQueues;
        this.transmissionQueues = transmissionFrameQueues;
        this.ackQueue = ackQueue;
        this.coreThread = coreThread;
        this.name = name;
        this.receiverID = receiverID;
    }
    
    @Override
    public void run()
    {
        Thread.currentThread().setName(name);
        
        while(true)
        {            
            try
            {
                if(Thread.interrupted())
                    throw new InterruptedException();
                
                DatagramPacket p = new DatagramPacket(new byte[MAX_PAYLOAD_LENGTH], MAX_PAYLOAD_LENGTH);
                this.dgramSocket.receive(p);
                try(
                    ByteArrayInputStream bain = new ByteArrayInputStream(p.getData());
                    GZIPInputStream gzin = new GZIPInputStream(bain);
                    ObjectInputStream oin = new ObjectInputStream(gzin);
                )
                {
                    Object obj = oin.readObject();
                    messageSwitch(obj);
                }
            }
            catch(IOException  disconnectionException)
            {
                LOG.info("Receiver entering termination phase: disconnection detected");
                dgramSocket.close();
                signalDisconnection();
                handleDisconnection(receiverID);
                LOG.info("Receiver terminated");
                return;
            }
            catch(ClassNotFoundException invalidMessageEx)
            {
                LOG.info("Receiver entering termination phase: invalid message received");
                signalDisconnection();
                handleDisconnection(receiverID);
                LOG.info("Receiver terminated");
                return;
            }
            catch(InterruptedException intEx)
            {
                LOG.info("Receiver disconnected: interruption received");
                return;
            }
        }
    }
    
    private void messageSwitch(Object obj) throws ClassNotFoundException
    {
        if(obj instanceof InputMessage)
        {
            InputMessage input = (InputMessage)obj;
            this.processInput(input);
        }
        else if(obj instanceof InputMessageArray)
        {
            InputMessageArray inputs = (InputMessageArray)obj;
            this.processInput(inputs);
        }
        else if(obj instanceof FrameACK)
        {
            FrameACK ack = (FrameACK)obj;
            this.processACK(ack);
        }
        else if(obj instanceof KeepAlive)
        {   
            //Socket connection timeout is reset at packet reception
        }
        else 
        {
            throw new ClassNotFoundException("Unrecognized message received");
        }
    }
    
    private void processInput(InputMessage input)
    {
        LOG.debug("1 InputMessage received from " + input.senderID + ": " + input.frame.getFrameNumber());
        ReceivingQueue receivingQueue = this.receivingQueues.get(input.senderID);
        FrameACK frameACK = receivingQueue.push(input.frame);
        frameACK.setSenderID(input.senderID);
        ackQueue.pushACKs(frameACK);

        if(input.frame.getCommand() instanceof DisconnectionSignal)
            handleDisconnection(input.senderID);
    }

    private void processInput(InputMessageArray inputs)
    {
        String numbers = "";
        for(FrameInput frame : inputs.frames)
            numbers += frame.getFrameNumber() + ", ";
        LOG.debug("" + inputs.frames.length + " InputMessages received from " + inputs.senderID + ": [ " + numbers + "]");
        ReceivingQueue receivingQueue = this.receivingQueues.get(inputs.senderID);
        FrameACK frameACK = receivingQueue.push(inputs.frames);
        frameACK.setSenderID(inputs.senderID);
        ackQueue.pushACKs(frameACK);
        
        if(inputs.frames[inputs.frames.length - 1].getCommand() instanceof DisconnectionSignal)
            handleDisconnection(inputs.senderID);
    }
    
    private void processACK(FrameACK ack)
    {
        TransmissionQueue transmissionFrameQueue = this.transmissionQueues.get(ack.senderID);
        transmissionFrameQueue.processACK(ack);
    }
    
    private void handleDisconnection(int disconnectedNode)
    {
        coreThread.disconnectTransmittingQueues(disconnectedNode);
    }

    private void signalDisconnection()
    {
        if(receiverID == RECEIVER_FROM_SERVER_ID)
        {
            for(ReceivingQueue receveingQueue : receivingQueues.values())
            {
                receveingQueue.push(new FrameInput(receveingQueue.getACK().cumulativeACK + 1, new DisconnectionSignal()));
            }
        }
        else   
        {
            ReceivingQueue receveingQueue = receivingQueues.get(receiverID);
            if(receveingQueue != null)
                receveingQueue.push(new FrameInput(receveingQueue.getACK().cumulativeACK + 1, new DisconnectionSignal()));
        }
    }
}
