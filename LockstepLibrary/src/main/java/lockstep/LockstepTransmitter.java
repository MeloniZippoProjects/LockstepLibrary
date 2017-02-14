/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lockstep;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Arrays;
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
 * @param <Command>
 */
public class LockstepTransmitter implements Runnable
{
    DatagramSocket dgramSocket;
    Map<Integer, TransmissionFrameQueue> transmissionFrameQueues;
    
    Semaphore transmissionSemaphore;
    long interTransmissionTimeout = 20;
    int maxPayloadLength = 512;
    
    boolean measuredMaxFramesInMessage = false;
    int maxFramesInMessage;
    
    
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

                    if(!measuredMaxFramesInMessage && frames.length > 0)
                        measureMaxFramesInMessage(frames[0]);                    
                    
                    if(frames.length == 1)
                    {
                        InputMessage msg = new InputMessage(entry.getKey(), frames[0]);
                        this.send(msg);
                        LOG.debug("1 message sent for " + entry.getKey());
                    }
                    else if(frames.length > 1)
                    {
                        if(frames.length > maxFramesInMessage)
                            frames = Arrays.copyOfRange(frames, 0, maxFramesInMessage);
                        
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

    private void send(InputMessageArray msg)
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

    private void measureMaxFramesInMessage(FrameInput testFrame)
    {
        int measuredPayloadLength = 0;
        try(
            ByteArrayOutputStream baout = new ByteArrayOutputStream();
            ObjectOutputStream oout = new ObjectOutputStream(baout);
        )
        {
            FrameInput[] frameInputs = new FrameInput[1];
            frameInputs[0] = testFrame;
            InputMessageArray inputMessageArray = new InputMessageArray(0, frameInputs);

            oout.writeObject(inputMessageArray);
            oout.flush();
            measuredPayloadLength = baout.toByteArray().length;
            LOG.debug("Payload measured as " + measuredPayloadLength);
            this.maxFramesInMessage = Math.floorDiv(maxPayloadLength, measuredPayloadLength);
            LOG.debug("maxFramesInMessage measured as " + maxFramesInMessage);
            measuredMaxFramesInMessage = true;
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}

