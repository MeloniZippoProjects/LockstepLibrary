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
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Map;
import lockstep.messages.simulation.FrameACK;
import org.apache.log4j.Logger;

/**
 *
 * @author Raff
 */
public class LockstepReceiver<Command extends Serializable> implements Runnable
{
    DatagramSocket dgramSocket;
    Map<Integer, ExecutionFrameQueue<Command>> executionFrameQueues;
    Map<Integer, TransmissionFrameQueue<Command>> transmissionFrameQueues;
    
    private static final Logger LOG = Logger.getLogger(LockstepReceiver.class.getName());
    
    public LockstepReceiver(DatagramSocket socket, Map<Integer, ExecutionFrameQueue<Command>> executionFrameQueues, Map<Integer, TransmissionFrameQueue<Command>> transmissionFrameQueues)
    {
        dgramSocket = socket;
        this.executionFrameQueues = executionFrameQueues;
        this.transmissionFrameQueues = transmissionFrameQueues;
    }
    
    @Override
    public void run()
    {
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
        LOG.debug("1 InputMessage received from " + input.hostID);
        ExecutionFrameQueue executionFrameQueue = this.executionFrameQueues.get(input.hostID);
        FrameACK frameAck = executionFrameQueue.push(input.frame);
        frameAck.setHostID(input.hostID);
        sendACK(frameAck);
    }

    private void processInput(InputMessageArray inputs)
    {
    LOG.debug("" + inputs.frames.length + " InputMessages received from " + inputs.hostID);
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