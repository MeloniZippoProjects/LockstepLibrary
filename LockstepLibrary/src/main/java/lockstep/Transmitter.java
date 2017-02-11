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
import lockstep.messages.InputMessage;

/**
 * 
 * @author Raff
 */
public class Transmitter implements Runnable
{
    DatagramSocket dgramSocket;
    Map<Integer, TransmissionFrameQueue> transmissionFrameQueues;
    
    public Transmitter(DatagramSocket socket, Map<Integer, TransmissionFrameQueue> transmissionFrameQueues)
    {
        //initialize members...
    }
    
    @Override
    public void run()
    {
        //Find most suitable wakeup/poll scheme...
        
        while(true)
        {
            try
            {
                for(Entry<Integer, TransmissionFrameQueue> entry : transmissionFrameQueues.entrySet())
                {
                    FrameInput[] frames = entry.getValue().pop();
                    for(FrameInput frame : frames)
                    {
                        InputMessage msg = new InputMessage(entry.getKey(), frame);
                        this.send(msg);
                    }
                }
                
                Thread.sleep(50); ///placeholder: think about timings, notification schemes, ect.
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
}
