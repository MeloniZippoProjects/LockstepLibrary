/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lockstep;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import lockstep.messages.simulation.InputMessage;
import lockstep.messages.simulation.InputMessageArray;
import org.apache.log4j.Logger;

/**
 * 
 * @author Raff
 * @param <Command>
 */
public class LockstepTransmitter<Command extends Serializable> implements Runnable
{
    DatagramSocket dgramSocket;
    Map<Integer, TransmissionFrameQueue<Command>> transmissionFrameQueues;
    
    Semaphore transmissionSemaphore;
    long interTransmissionTimeout = 20;
    static final int maxPayloadLength = 512;
    final String name;
    
    private static final Logger LOG = Logger.getLogger(LockstepTransmitter.class.getName());
    
    public LockstepTransmitter(DatagramSocket socket, Map<Integer, TransmissionFrameQueue<Command>> transmissionFrameQueues, Semaphore transmissionSemaphore, String name)
    {
        this.dgramSocket = socket;
        this.transmissionFrameQueues = transmissionFrameQueues;
        this.transmissionSemaphore = transmissionSemaphore;
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
                for(TransmissionFrameQueue txQ : transmissionFrameQueues.values())
                {   
                    LOG.debug(txQ);
                }
                LOG.debug("Try acquire semaphore");
                if(!transmissionSemaphore.tryAcquire(interTransmissionTimeout, TimeUnit.MILLISECONDS))
                {
                    LOG.debug("Transmission timeout reached");
                }                
                LOG.debug("Out of Semaphore");
                for(Entry<Integer, TransmissionFrameQueue<Command>> transmissionQueueEntry : transmissionFrameQueues.entrySet())
                {
                    int senderID = transmissionQueueEntry.getKey();
                    
                    LOG.debug("Entry " + senderID);
                    FrameInput[] frames = transmissionQueueEntry.getValue().pop();                  
                    
                    if(frames.length == 1)
                    {
                        InputMessage msg = new InputMessage(senderID, frames[0]);
                        this.send(msg);
                        LOG.debug("1 message sent for " + senderID);
                    }
                    else if(frames.length > 1)
                    {
                        this.send(senderID, frames);
                    }
                }
                transmissionSemaphore.drainPermits();
                LOG.debug("Drained permits from semaphore");
            }
            catch(InterruptedException e)
            {
                //Shutdown signal... may be changed
                return;
            }
        }
    }
    
    private void send(InputMessage msg)
    {
        try(
                ByteArrayOutputStream baout = new ByteArrayOutputStream();
                ObjectOutputStream oout = new ObjectOutputStream(baout);
        )
        {
            oout.writeObject(msg);
            oout.flush();
            byte[] data = baout.toByteArray();
            this.dgramSocket.send(new DatagramPacket(data, data.length));
            LOG.debug("Payload size " + data.length);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }       

    private void send(int senderID, FrameInput[] frames)
    {
        int payloadLength = maxPayloadLength + 1;
        int framesToInclude = frames.length + 1;
        byte[] payload = null;
        while( payloadLength > maxPayloadLength && framesToInclude > 0)
        {
            try(
                ByteArrayOutputStream baout = new ByteArrayOutputStream();
                ObjectOutputStream oout = new ObjectOutputStream(baout);
            )
            {
                framesToInclude--;
                FrameInput[] framesToSend = Arrays.copyOf(frames, framesToInclude);
                InputMessageArray inputMessageArray = new InputMessageArray(senderID, framesToSend);
                oout.writeObject(inputMessageArray);
                oout.flush();
                payload = baout.toByteArray();
                payloadLength = payload.length;
            }
            catch(IOException e)
            {
                LOG.fatal(e.getStackTrace());
                System.exit(1);
            }
        }
        
        try
        {
            this.dgramSocket.send(new DatagramPacket(payload, payload.length));
        } catch (IOException ex)
        {
            LOG.fatal("Can't send dgramsocket");
            System.exit(1);
        }
        LOG.debug("" + framesToInclude + "sent for " + senderID);
        LOG.debug("Payload size " + payloadLength);
        
        if(framesToInclude < frames.length)
        {
            frames = Arrays.copyOfRange(frames, framesToInclude, frames.length);
            send(senderID, frames);
        }
    }
}

