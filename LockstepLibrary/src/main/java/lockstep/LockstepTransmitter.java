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
import java.net.SocketException;
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
    ACKSet ackQueue;
    
    long interTransmissionTimeout;
    static final int maxPayloadLength = 300;
    final String name;
    
    private static final Logger LOG = LogManager.getLogger(LockstepTransmitter.class);
    private final int tickrate;
    
    final boolean sendKeepAliveSwitch;
    final int keepAliveTicksTimeout;
    boolean terminationPhase = false;
    
    public static class Builder {

        private DatagramSocket dgramSocket;
        private Map<Integer,TransmissionQueue> transmissionQueues;
        private ACKSet ackQueue;
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

        public Builder ackSet(final ACKSet value) {
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
    
    public LockstepTransmitter(DatagramSocket socket, int tickrate, int keepAliveTimeout, Map<Integer, TransmissionQueue> transmissionQueues, String name, ACKSet ackQueue)
    {
        if(socket.isClosed())
            throw new IllegalArgumentException("Socket is closed");
        else
            this.dgramSocket = socket;
        
        if(tickrate <= 0)
            throw new IllegalArgumentException("Tickrate must be an integer greater than 0");
        else
            this.tickrate = tickrate;
        
        
        if(transmissionQueues == null)
            throw new IllegalArgumentException("Transmission Queues Map cannot be null");
        else
            this.transmissionQueues = transmissionQueues;
        
        if(name != null)
            this.name = name;
        else
            this.name = "Transmitter";
        
        if(ackQueue == null)
            throw new IllegalArgumentException("Ack Queue cannot be null");
        else
            this.ackQueue = ackQueue;
        
        
        this.interTransmissionTimeout = 3*(1000/tickrate);
        
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
                
                if(dgramSocket.isClosed())
                    throw new SocketException();
                                
                boolean sentCommands = processCommands();
                boolean sentACKs = processACKs();
                
                boolean sentSomething = sentCommands || sentACKs;
                
                if(sendKeepAliveSwitch && !sentSomething)
                    sendKeepAlive();
                
                Thread.sleep(1000/tickrate);
            }
            catch(InterruptedException intEx)
            {                
                LOG.info("Transmitter entering termination phase: interruption received");
                if(dgramSocket.isClosed())
                {
                    LOG.info("Transmitter terminating: connection already closed");
                    return;
                }
                else
                    terminationPhase = true;
            }
            catch(TransmissionCompletedException trEx)
            {
                LOG.info("Transmitter terminating: transimission completed. Proceding to close the socket");
                dgramSocket.close();
                return;
            }
            catch(IOException ioEx)
            {
                LOG.info("Transmitter disconnected: socket failure");
                dgramSocket.close(); //Forcing failure on receiver too
                return;
            }            
        }
    }
    
    private void sendKeepAlive() throws IOException
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
        catch(IOException ioEx)
        {
            throw ioEx;
        }
        LOG.info("Transmitter sent a keep alive");
    }
    

    private boolean processCommands() throws IOException
    {
        boolean sentSomething = false;
        for(Entry<Integer, TransmissionQueue> transmissionQueueEntry : transmissionQueues.entrySet())
        {
            if(transmissionQueueEntry.getValue().hasFramesToSend())
            {
                sentSomething = true;
                int senderID = transmissionQueueEntry.getKey();
                
                FrameInput[] frames = transmissionQueueEntry.getValue().pop();
                
                if(frames.length == 1)
                {
                    InputMessage msg = new InputMessage(senderID, frames[0]);
                    this.send(msg);
                }
                else if(frames.length > 1)
                {
                    this.send(senderID, frames);
                }
            }
        }
        
        if(!sentSomething && terminationPhase)
            throw new TransmissionCompletedException();
        
        return sentSomething;
    }
    
    private boolean processACKs() throws IOException
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
    
    private void sendACK(FrameACK frameACK) throws IOException
    {
        if(frameACK.selectiveACKs == null || frameACK.selectiveACKs.length == 0) 
            sendSingleACK(frameACK);
        else
            sendSplitACKs(frameACK);
    }

    private void sendSingleACK(FrameACK frameACK) throws IOException
    {
        try(
            ByteArrayOutputStream baout = new ByteArrayOutputStream();
            GZIPOutputStream gzout = new GZIPOutputStream(baout);
            ObjectOutputStream oout = new ObjectOutputStream(gzout);
        ){
            oout.writeObject(frameACK);
            oout.flush();
            gzout.finish();
            byte[] data = baout.toByteArray();
            this.dgramSocket.send(new DatagramPacket(data, data.length));
        } catch (IOException ioEx)
        {
            throw ioEx;
        }
    }
    
    private void sendSplitACKs(FrameACK frameACK) throws IOException
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
            catch(IOException ioEx)
            {
                throw ioEx;
            }
        }
        
        this.dgramSocket.send(new DatagramPacket(payload, payload.length));

        if(selectiveACKsToInclude < selectiveACKs.length)
        {
            frameACK.selectiveACKs = Arrays.copyOfRange(selectiveACKs, selectiveACKsToInclude, selectiveACKs.length);
            sendSplitACKs(frameACK);
        }
    }
    
    private void send(InputMessage msg) throws IOException
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
        }
        catch(IOException ioEx)
        {
            throw ioEx;
        }
    }       

    private void send(int senderID, FrameInput[] frames) throws IOException
    {
        int payloadLength = maxPayloadLength + 1;
        int framesToInclude = frames.length + 1;
        byte[] payload = null;
        InputMessageArray inputMessageArray;
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
            catch(IOException ioEx)
            {
                throw ioEx;
            }
        }
                
        this.dgramSocket.send(new DatagramPacket(payload, payload.length));
        
        if(framesToInclude < frames.length)
        {
            frames = Arrays.copyOfRange(frames, framesToInclude, frames.length);
            send(senderID, frames);
        }
    }
}

