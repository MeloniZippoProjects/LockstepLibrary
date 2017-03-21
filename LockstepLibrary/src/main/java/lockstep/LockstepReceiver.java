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
import java.util.Arrays;
import java.util.Map;
import java.util.logging.Level;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import lockstep.messages.simulation.FrameACK;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.log4j.Logger;

/**
 *
 * @author Raff
 */
public class LockstepReceiver<Command extends Serializable> implements Runnable
{
    volatile DatagramSocket dgramSocket;
    volatile Map<Integer, ReceivingQueue<Command>> receivingQueues;
    volatile Map<Integer, TransmissionQueue<Command>> transmissionFrameQueues;
    volatile ACKQueue ackQueue;
    static final int maxPayloadLength = 300;
    
    private static final Logger LOG = Logger.getLogger(LockstepReceiver.class.getName());
    
    private final String name;
    
    private final int tickrate;
    
    public LockstepReceiver(DatagramSocket socket, int tickrate, Map<Integer, ReceivingQueue<Command>> receivingQueues, Map<Integer, TransmissionQueue<Command>> transmissionFrameQueues, String name, ACKQueue ackQueue)
    {
        dgramSocket = socket;
        this.receivingQueues = receivingQueues;
        this.transmissionFrameQueues = transmissionFrameQueues;
        this.name = name;
        this.tickrate = tickrate;
        this.ackQueue = ackQueue;
    }
    
    @Override
    public void run()
    {
        Thread.currentThread().setName(name);
        
        while(true)
        {
            try
            {
                DatagramPacket p = new DatagramPacket(new byte[maxPayloadLength], maxPayloadLength);
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
            catch(Exception e)
            {
                e.printStackTrace();
            }
            
            /*
            try {
                Thread.sleep(1000/tickrate);
                
                //Study shutdown case
            } catch (InterruptedException ex) {
                java.util.logging.Logger.getLogger(LockstepReceiver.class.getName()).log(Level.SEVERE, null, ex);
            }
            */
        }
    }
    
    private void messageSwitch(Object obj) throws Exception
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
        LOG.debug("1 InputMessage received from " + input.senderID + ": " + input.frame.getFrameNumber());
        ReceivingQueue receivingQueue = this.receivingQueues.get(input.senderID);
        FrameACK frameACK = receivingQueue.push(input.frame);
        frameACK.setSenderID(input.senderID);
        ackQueue.pushACKs(frameACK);
        //sendACK(frameACK);
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
        
        //sendACK(frameACK);
    }
    
    private void processACK(FrameACK ack)
    {
        TransmissionQueue transmissionFrameQueue = this.transmissionFrameQueues.get(ack.senderID);
        transmissionFrameQueue.processACK(ack);
    }
    
    
}
