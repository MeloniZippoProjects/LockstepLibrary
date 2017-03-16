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
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import lockstep.messages.simulation.FrameACK;
import org.apache.log4j.Logger;

/**
 *
 * @author Raff
 */
public class LockstepReceiver<Command extends Serializable> implements Runnable
{
    volatile DatagramSocket dgramSocket;
    volatile Map<Integer, ExecutionFrameQueue<Command>> executionFrameQueues;
    volatile Map<Integer, TransmissionFrameQueue<Command>> transmissionFrameQueues;
    
    static final int maxPayloadLength = 512;
    
    private static final Logger LOG = Logger.getLogger(LockstepReceiver.class.getName());
    
    private final String name;
    
    public LockstepReceiver(DatagramSocket socket, Map<Integer, ExecutionFrameQueue<Command>> executionFrameQueues, Map<Integer, TransmissionFrameQueue<Command>> transmissionFrameQueues, String name)
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
            
            //Study shutdown case
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
        ExecutionFrameQueue executionFrameQueue = this.executionFrameQueues.get(input.senderID);
        FrameACK frameACK = executionFrameQueue.push(input.frame);
        frameACK.setSenderID(input.senderID);
        sendACK(frameACK);
    }

    private void processInput(InputMessageArray inputs)
    {
        String numbers = "";
        for(FrameInput frame : inputs.frames)
            numbers += frame.getFrameNumber() + ", ";
        LOG.debug("" + inputs.frames.length + " InputMessages received from " + inputs.senderID + ": [ " + numbers + "]");
        ExecutionFrameQueue executionFrameQueue = this.executionFrameQueues.get(inputs.senderID);
        FrameACK frameACK = executionFrameQueue.push(inputs.frames);
        frameACK.setSenderID(inputs.senderID);
        sendACK(frameACK);
    }
    
    private void processACK(FrameACK ack)
    {
        TransmissionFrameQueue transmissionFrameQueue = this.transmissionFrameQueues.get(ack.senderID);
        transmissionFrameQueue.processACK(ack);
    }
    
    private void sendACK(FrameACK frameACK)
    {
        if(frameACK.selectiveACKs == null || frameACK.selectiveACKs.length == 0) 
            sendSingleACK(frameACK);
        else
            sendSplitACKs(frameACK);
    }

    private void sendSingleACK(FrameACK frameACK)
    {
        try(
            ByteArrayOutputStream baout = new ByteArrayOutputStream();
            GZIPOutputStream gzout = new GZIPOutputStream(baout);
            ObjectOutputStream oout = new ObjectOutputStream(gzout);
        )
        {
            oout.writeObject(frameACK);
            oout.flush();
            gzout.finish();
            byte[] data = baout.toByteArray();
            this.dgramSocket.send(new DatagramPacket(data, data.length));
            LOG.debug("Single ACK sent, payload size:" + data.length);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
    
    private void sendSplitACKs(FrameACK frameACK)
    {
        int payloadLength = maxPayloadLength + 1;
        int[] selectiveACKs = frameACK.selectiveACKs;
        int selectiveACKsToInclude = frameACK.selectiveACKs.length + 1;
        byte[] payload = null;
        while( payloadLength > maxPayloadLength && selectiveACKsToInclude > 0)
        {
            try(
                ByteArrayOutputStream baout = new ByteArrayOutputStream();
                GZIPOutputStream gzout = new GZIPOutputStream(baout);
                ObjectOutputStream oout = new ObjectOutputStream(gzout);
            )
            {
                selectiveACKsToInclude--;
                frameACK.selectiveACKs = Arrays.copyOf(selectiveACKs, selectiveACKsToInclude);
                oout.writeObject(frameACK);
                oout.flush();
                gzout.finish();
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
        }
        catch(IOException e)
        {
            LOG.fatal("Can't send dgramsocket");
            System.exit(1);
        }
        LOG.debug("" + selectiveACKsToInclude + " selectiveACKs sent");
        LOG.debug("Payload size " + payloadLength);
        
        LOG.debug("SelectiveACKsToInclude = " + selectiveACKsToInclude);
        LOG.debug("SelectiveACKs.length = .... " + selectiveACKs.length);
        if(selectiveACKsToInclude < selectiveACKs.length)
        {
            LOG.debug("SPlitting acks");
            frameACK.selectiveACKs = Arrays.copyOfRange(selectiveACKs, selectiveACKsToInclude, selectiveACKs.length);
            if(frameACK.selectiveACKs == null)
                LOG.debug("selectiveacks è diventato null");
            sendSplitACKs(frameACK);
        }
    }
}
