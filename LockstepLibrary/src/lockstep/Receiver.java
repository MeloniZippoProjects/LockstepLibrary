/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lockstep;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Map;
import lockstep.messages.FrameACK;
import lockstep.messages.InputMessage;

/**
 *
 * @author Raff
 */
public class Receiver implements Runnable
{
    DatagramSocket dgramSocket;
    Map<Integer, ExecutionFrameQueue> executionFrameQueues;
    Map<Integer, TransmissionFrameQueue> transmissionFrameQueues;
    
    public Receiver(DatagramSocket socket, Map<Integer, ExecutionFrameQueue> executionFrameQueues, Map<Integer, TransmissionFrameQueue> transmissionFrameQueues)
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
               ByteArrayInputStream bain = new ByteArrayInputStream(p.getData());
               ObjectInputStream oin = new ObjectInputStream(bain);
               
               Object obj = oin.readObject();
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
        else if(obj instanceof InputMessage[])
        {
            InputMessage[] inputs = (InputMessage[])obj;
            for(InputMessage input : inputs)
                this.processInput(input);
        }
        else if(obj instanceof FrameACK)
        {
            FrameACK ack = (FrameACK)obj;
            this.processACK(ack);
        }
        else if(obj instanceof FrameACK[])
        {
            FrameACK[] acks = (FrameACK[])obj;
            for(FrameACK ack : acks)
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
        executionFrameQueue.push(input.frame);
    }
    
    private void processACK(FrameACK ack)
    {
        TransmissionFrameQueue transmissionFrameQueue = this.transmissionFrameQueues.get(ack.hostID);
        transmissionFrameQueue.processACK(ack);
    }
    
}
