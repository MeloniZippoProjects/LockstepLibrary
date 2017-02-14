/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lockstep;

import lockstep.messages.simulation.InputMessageArray;
import lockstep.messages.simulation.InputMessage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Map;
import lockstep.messages.simulation.FrameACK;
import org.apache.log4j.Logger;

/**
 *
 * @author Raff
 */
public class LockstepReceiver implements Runnable
{
    DatagramSocket dgramSocket;
    Map<Integer, ExecutionFrameQueue> executionFrameQueues;
    Map<Integer, TransmissionFrameQueue> transmissionFrameQueues;
    
    private static final Logger LOG = Logger.getLogger(LockstepReceiver.class.getName());
    
    private final String name;
    
    public LockstepReceiver(DatagramSocket socket, Map<Integer, ExecutionFrameQueue> executionFrameQueues, Map<Integer, TransmissionFrameQueue> transmissionFrameQueues, String name)
    {
        dgramSocket = socket;
        this.executionFrameQueues = executionFrameQueues;
        this.transmissionFrameQueues = transmissionFrameQueues;
        this.name = name;
    }
    
    @Override
    public void run()
    {
        Thread.currentThread().setName(name);
        
        while(true)
        {
            try
            {
               DatagramPacket p = new DatagramPacket(new byte[512], 512);
               this.dgramSocket.receive(p);
               ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(p.getData());
               ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
               
               Object obj = objectInputStream.readObject();
               
               messageDispatch(obj);               
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
            
            //Study shutdown case
        }
    }
    
    private void messageDispatch(Object obj) throws Exception
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
        else 
        {
            throw(new Exception("Unrecognized message received"));
        }
    }
    
    private void processInput(InputMessage input)
    {
        LOG.debug("1 InputMessage received from " + input.hostID + ": " + input.frame.frameNumber);
        ExecutionFrameQueue executionFrameQueue = this.executionFrameQueues.get(input.hostID);
        FrameACK frameAck = executionFrameQueue.push(input.frame);
        frameAck.setHostID(input.hostID);
        sendACK(frameAck);
    }

    private void processInput(InputMessageArray inputs)
    {
        String numbers = "";
        for(FrameInput frame : inputs.frames)
            numbers += frame.frameNumber + ", ";
        LOG.debug("" + inputs.frames.length + " InputMessages received from " + inputs.hostID + ": [ " + numbers + "]");
        ExecutionFrameQueue executionFrameQueue = this.executionFrameQueues.get(inputs.hostID);
        FrameACK frameAck = executionFrameQueue.push(inputs.frames);
        frameAck.setHostID(inputs.hostID);
        sendACK(frameAck);
    }
    
    private void processACK(FrameACK ack)
    {
        TransmissionFrameQueue transmissionFrameQueue = this.transmissionFrameQueues.get(ack.hostID);
        transmissionFrameQueue.processACK(ack);
    }

    private void sendACK(FrameACK ack)
    {
        try(
            ByteArrayOutputStream baout = new ByteArrayOutputStream();
            ObjectOutputStream oout = new ObjectOutputStream(baout);
        )
        {
            oout.writeObject(ack);
            oout.flush();
            byte[] data = baout.toByteArray();
            this.dgramSocket.send(new DatagramPacket(data, data.length));
            LOG.debug("ACK sent, payload size:" + data.length);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}