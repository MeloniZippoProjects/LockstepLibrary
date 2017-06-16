/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lockstep;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.GZIPOutputStream;
import lockstep.messages.simulation.FrameACK;
import lockstep.messages.simulation.InputMessage;
import lockstep.messages.simulation.InputMessageArray;
import lockstep.messages.simulation.KeepAlive;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class LockstepTransmitter extends Thread
{
    DatagramSocket dgramSocket;
    Map<Integer, TransmissionQueue> transmissionQueues;
    ACKQueue ackQueue;
    
    long interTransmissionTimeout;
    static final int maxPayloadLength = 300;
    final String name;
    
    private static final Logger LOG = LogManager.getLogger(LockstepTransmitter.class);
    private final int tickrate;
    
    final boolean sendKeepAliveSwitch;
    final int keepAliveTicksTimeout;

    public static class Builder {

        private DatagramSocket dgramSocket;
        private Map<Integer,TransmissionQueue> transmissionQueues;
        private ACKQueue ackQueue;
        private String name;
        private int tickrate;
        private int keepAliveTimeout;

        private Builder() {
        }

        public Builder dgramSocket(final DatagramSocket value) {
            this.dgramSocket = value;
            return this;
        }

        public Builder transmissionQueues(final Map<Integer,TransmissionQueue> value) {
            this.transmissionQueues = value;
            return this;
        }

        public Builder ackQueue(final ACKQueue value) {
            this.ackQueue = value;
            return this;
        }
        public Builder name(final String value) {
            this.name = value;
            return this;
        }

        public Builder tickrate(final int value) {
            this.tickrate = value;
            return this;
        }

        public Builder keepAliveTimeout(final int keepAliveTimeout)
        {
            this.keepAliveTimeout = keepAliveTimeout;
            return this;
        }

        public LockstepTransmitter build() {
            return new LockstepTransmitter(dgramSocket, tickrate,
                    keepAliveTimeout, transmissionQueues,
                    name, ackQueue);
        }
    }

    public static LockstepTransmitter.Builder builder() {
        return new LockstepTransmitter.Builder();
    }
    
    public LockstepTransmitter(DatagramSocket socket, int tickrate, int keepAliveTimeout, Map<Integer, TransmissionQueue> transmissionQueues, String name, ACKQueue ackQueue)
    {
        this.dgramSocket = socket;
        this.tickrate = tickrate;
        this.interTransmissionTimeout = 3*(1000/tickrate);
        this.transmissionQueues = transmissionQueues;
        this.name = name;
        this.ackQueue = ackQueue;
        
        if(keepAliveTimeout <= 0)
        {
            this.sendKeepAliveSwitch = false;
            keepAliveTicksTimeout = 0;
        }
        else
        {
            this.sendKeepAliveSwitch = true;
            int tempKeepAliveTicksTimeout = Math.round(keepAliveTimeout / (1000/tickrate));
            this.keepAliveTicksTimeout = (tempKeepAliveTicksTimeout > 1) ? tempKeepAliveTicksTimeout : 1;
        }
    }
    
    @Override
    public void run()
    {        
        Thread.currentThread().setName(name);
        
        while(true)
        {
            try
            {
                if(Thread.interrupted())
                    throw new InterruptedException();
                
                for(TransmissionQueue txQ : transmissionQueues.values())
                {   
                    LOG.debug(txQ);
                }
                
                boolean sentCommands = processCommands();
                boolean sentACKs = processACKs();
                
                boolean sentSomething = sentCommands || sentACKs;
                
                if(sendKeepAliveSwitch && !sentSomething)
                    sendKeepAlive();
                
                Thread.sleep(1000/tickrate);
            }
            catch(InterruptedException e)
            {
                LOG.info("Transmitter disconnected");
                return;
            }
        }
    }
    
    private void sendKeepAlive()
    {
        try(
            ByteArrayOutputStream baout = new ByteArrayOutputStream();
            GZIPOutputStream gzout = new GZIPOutputStream(baout);
            ObjectOutputStream oout = new ObjectOutputStream(gzout);
        )
        {
            oout.writeObject(new KeepAlive());
            oout.flush();
            gzout.finish();
            byte[] payload = baout.toByteArray();
            dgramSocket.send(new DatagramPacket(payload, payload.length));
        }
        catch(IOException e)
        {
            e.printStackTrace();

            LOG.fatal(e.getStackTrace());
            System.exit(1);
        }
        LOG.info("SENT KEEP ALIVE");
    }
    

    private boolean processCommands() {
        boolean sentSomething = false;
        for(Entry<Integer, TransmissionQueue> transmissionQueueEntry : transmissionQueues.entrySet())
        {
            if(transmissionQueueEntry.getValue().hasFramesToSend())
            {
                sentSomething = true;
                int senderID = transmissionQueueEntry.getKey();
                
                LOG.debug("Entry " + senderID);
                FrameInput[] frames = transmissionQueueEntry.getValue().pop();
                
                LOG.debug("txq " + senderID + "has to send: ");
                for(int i = 0; i < frames.length; ++i)
                {
                    LOG.debug("Frame " + i + ": " + frames[i].getFrameNumber());
                }
                
                
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
        }
        return sentSomething;
    }
    
    private boolean processACKs()
    {
        FrameACK[] acks = ackQueue.getACKs();
        boolean sentSomething = false;
        
        for(FrameACK ack : acks)
        {
            sendACK(ack);
            sentSomething = true;
        }
        
        return sentSomething;
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
            LOG.debug("["+frameACK.senderID+"] I just ACKed ("+data.length+"): up to " + frameACK.cumulativeACK + " and " + ArrayUtils.toString(frameACK.selectiveACKs));
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
                e.printStackTrace();
                System.exit(1);
            }
        }
        
        LOG.debug("["+frameACK.senderID+"] I just Acked ("+payload.length+"): up to " + frameACK.cumulativeACK + "and " + ArrayUtils.toString(frameACK.selectiveACKs));

        try
        {
            this.dgramSocket.send(new DatagramPacket(payload, payload.length));
        }
        catch(IOException e)
        {
            LOG.fatal("Can't send dgramsocket");
            e.printStackTrace();
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
    
    private void send(InputMessage msg)
    {
        try(
                ByteArrayOutputStream baout = new ByteArrayOutputStream();
                GZIPOutputStream gzout = new GZIPOutputStream(baout);
                ObjectOutputStream oout = new ObjectOutputStream(gzout);
        )
        {
            oout.writeObject(msg);
            oout.flush();
            gzout.finish();
            byte[] data = baout.toByteArray();
            this.dgramSocket.send(new DatagramPacket(data, data.length));
            LOG.debug("Payload size " + data.length);
            LOG.debug("["+msg.senderID+"] I just sent ("+data.length+"): " + msg);
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
        InputMessageArray inputMessageArray = null;
        while( payloadLength > maxPayloadLength && framesToInclude > 0)
        {
            try(
                ByteArrayOutputStream baout = new ByteArrayOutputStream();
                GZIPOutputStream gzout = new GZIPOutputStream(baout);
                ObjectOutputStream oout = new ObjectOutputStream(gzout);
            )
            {
                framesToInclude--;
                FrameInput[] framesToSend = Arrays.copyOf(frames, framesToInclude);
                inputMessageArray = new InputMessageArray(senderID, framesToSend);
                oout.writeObject(inputMessageArray);
                oout.flush();
                gzout.finish();
                payload = baout.toByteArray();
                payloadLength = payload.length;
            }
            catch(IOException e)
            {
                            e.printStackTrace();

                LOG.fatal(e.getStackTrace());
                System.exit(1);
            }
        }
        
        LOG.debug("["+senderID+"] I just sent: ("+payload.length+")" + inputMessageArray);
        
        try
        {
            this.dgramSocket.send(new DatagramPacket(payload, payload.length));
        } catch (IOException ex)
        {
                        ex.printStackTrace();

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

