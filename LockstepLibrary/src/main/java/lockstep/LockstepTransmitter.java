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
import lockstep.messages.simulation.InputMessage;

/**
 * 
 * @author Raff
 */
public class LockstepTransmitter implements Runnable
{
    DatagramSocket dgramSocket;
    Map<Integer, TransmissionFrameQueue> transmissionFrameQueues;
    
    QueueAvailability transmissionFrameQueuesReady;

    public LockstepTransmitter(DatagramSocket socket, Map<Integer, TransmissionFrameQueue> transmissionFrameQueues)
    {
        this.dgramSocket = socket;
        this.transmissionFrameQueues = transmissionFrameQueues;
    }
    
    @Override
    public void run()
    {        
        while(true)
        {
            try
            {
                synchronized(transmissionFrameQueuesReady)
                {
                    while(transmissionFrameQueuesReady.equals(Boolean.FALSE))
                        wait();

                    for(Entry<Integer, TransmissionFrameQueue> entry : transmissionFrameQueues.entrySet())
                    {
                        FrameInput[] frames = entry.getValue().pop();
                        for(FrameInput frame : frames)
                        {
                            InputMessage msg = new InputMessage(entry.getKey(), frame);
                            this.send(msg);
                        }
                    }

                    //This efficient as we assume that interframetime < rtt
                    transmissionFrameQueuesReady.setValue(Boolean.FALSE);    
                }
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

    public void signalTransmissionFrameQueuesReady()
    {
        synchronized(transmissionFrameQueuesReady)
        {
            transmissionFrameQueuesReady.setValue(Boolean.TRUE);
            notify();
        }
    }
}

