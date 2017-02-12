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

/**
 *
 * @author Raff
 */
public class LockstepReceiver implements Runnable
{
    DatagramSocket dgramSocket;
    Map<Integer, ExecutionFrameQueue> executionFrameQueues;
    Map<Integer, TransmissionFrameQueue> transmissionFrameQueues;
    
    public LockstepReceiver(DatagramSocket socket, Map<Integer, ExecutionFrameQueue> executionFrameQueues, Map<Integer, TransmissionFrameQueue> transmissionFrameQueues)
    {
        //initialize members...
    }
    
    @Override
    public void run()
    {
        while(true)
        {
            try
            {
               DatagramPacket p = new DatagramPacket(new byte[1024], 1024);
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
        ExecutionFrameQueue executionFrameQueue = this.executionFrameQueues.get(input.hostID);
        FrameACK frameAck = executionFrameQueue.push(input.frame);
        frameAck.setHostID(input.hostID); //forse dovrebbe essere il proprio id
        sendACK(frameAck);
    }

    private void processInput(InputMessageArray inputs)
    {
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
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
        )
        {
            objectOutputStream.writeObject(ack);
            byte[] data = byteArrayOutputStream.toByteArray();
            this.dgramSocket.send(new DatagramPacket(data, data.length));
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}