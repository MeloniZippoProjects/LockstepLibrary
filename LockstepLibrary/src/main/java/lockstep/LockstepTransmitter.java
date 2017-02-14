/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lockstep;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import lockstep.messages.simulation.InputMessage;
import lockstep.messages.simulation.InputMessageArray;
import org.apache.log4j.Logger;

/**
 * 
 * @author Raff
 */
public class LockstepTransmitter implements Runnable
{
    DatagramSocket dgramSocket;
    Map<Integer, TransmissionFrameQueue> transmissionFrameQueues;
    
    Semaphore transmissionSemaphore;
    long interTransmissionTimeout = 20;

    private static final Logger LOG = Logger.getLogger(LockstepTransmitter.class.getName());
    
    public LockstepTransmitter(DatagramSocket socket, Map<Integer, TransmissionFrameQueue> transmissionFrameQueues, Semaphore transmissionSemaphore)
    {
        this.dgramSocket = socket;
        this.transmissionFrameQueues = transmissionFrameQueues;
        this.transmissionSemaphore = transmissionSemaphore;
    }
    
    @Override
    public void run()
    {        
        while(true)
        {
            try
            {
                if(!transmissionSemaphore.tryAcquire(interTransmissionTimeout, TimeUnit.MILLISECONDS))
                {
                    LOG.trace("Transmission timeout reached");
                }                
                
                for(Entry<Integer, TransmissionFrameQueue> entry : transmissionFrameQueues.entrySet())
                {
                    FrameInput[] frames = entry.getValue().pop();
                    if(frames.length == 1)
                    {
                        InputMessage msg = new InputMessage(entry.getKey(), frames[0]);
                        this.send(msg);
                        LOG.debug("1 message sent for " + entry.getKey());
                    }
                    else if(frames.length > 1)
                    {
                        InputMessageArray msg = new InputMessageArray(entry.getKey(), frames);
                        this.send(msg);
                        LOG.debug("" + frames.length + " messages sent for " + entry.getKey());
                    }
                }
                transmissionSemaphore.drainPermits();
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
            byte[] data = baout.toByteArray();
            this.dgramSocket.send(new DatagramPacket(data, data.length));
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }       

    private void send(InputMessageArray msg)
    {
        try(
                ByteArrayOutputStream baout = new ByteArrayOutputStream();
                ObjectOutputStream oout = new ObjectOutputStream(baout);
        )
        {
            oout.writeObject(msg);
            byte[] data = baout.toByteArray();
            this.dgramSocket.send(new DatagramPacket(data, data.length));
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}

