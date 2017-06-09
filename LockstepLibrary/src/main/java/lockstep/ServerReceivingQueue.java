/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lockstep;

import java.io.Serializable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import lockstep.messages.simulation.DisconnectionSignal;
import lockstep.messages.simulation.FrameACK;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * This frame queue supports out of order and simultaneous insertion, amd single
 * extraction of the first available frame. 
 * A semaphore is released when a frame input is available.
 * 
 * It is thread safe.
 * @param <Command> Application class containing the data to transmit
 */

public class ServerReceivingQueue<Command extends Serializable> implements ReceivingQueue<Command>
{
    
    private final int senderID;
    
    ConcurrentSkipListMap<Integer, Serializable> commandBuffer;
    Semaphore executionSemaphore;
        
    AtomicInteger lastInOrderACK;
    ConcurrentSkipListSet<Integer> selectiveACKsSet;
    
    //AtomicInteger lastInOrderProcessed;
    //ConcurrentSkipListSet<Integer> selectiveProcessedSet;
    
    private static final Logger LOG = LogManager.getLogger(ClientReceivingQueue.class);
    
    /**
     * Constructor.
     * 
     * @param initialFrameNumber First frame's number. Must be the same for all 
     * the hosts using the protocol
     * 
     * @param senderID ID of the client whose frames are collected in this queue
     * 
     * @param serverExecutionSemaphore semaphore used by to signal the client of
     * the availability of the next frame input. The client awaits that all the 
     * queues are ready before collecting the next frame inputs
     */
    public ServerReceivingQueue(int initialFrameNumber, int senderID, Semaphore serverExecutionSemaphore)
    {
        this.senderID = senderID;
    
        this.commandBuffer = new ConcurrentSkipListMap<>();
        this.executionSemaphore = serverExecutionSemaphore;

        this.lastInOrderACK = new AtomicInteger(initialFrameNumber - 1);
        this.selectiveACKsSet = new ConcurrentSkipListSet<>();
        
        //this.lastInOrderProcessed = new AtomicInteger(initialFrameNumber - 1);
        //this.selectiveProcessedSet = new ConcurrentSkipListSet<>();
        
        System.out.println("BufferHead["+senderID+"] initialized at " + initialFrameNumber);
    }
    
    /**
     * Extracts the first available frame input. 
     * This method will change the queue, extracting the first packet if present.
     * 
     * @return the next available frame input, or null if not present.
     */
    @Override
    public FrameInput pop()
    {        
        Entry<Integer, Serializable> firstFrameEntry = commandBuffer.pollFirstEntry();
        
        if( firstFrameEntry != null )
        {
//            if(firstFrameEntry.getKey() == this.lastInOrderProcessed.get() + 1)
//            {
//                lastInOrderProcessed.incrementAndGet();
//                
//                while(!selectiveProcessedSet.isEmpty() && selectiveProcessedSet.first() == (lastInOrderProcessed.get() + 1))
//                {
//                    lastInOrderProcessed.incrementAndGet();
//                    selectiveProcessedSet.removeAll(selectiveProcessedSet.headSet(lastInOrderProcessed.get(), true));
//                }
//            }
            
            if(commandBuffer.firstEntry() != null)
                executionSemaphore.release();
            
            return new FrameInput(firstFrameEntry.getKey(), firstFrameEntry.getValue());
        }
        else
        {
            return null;
        }        
    }
    
    /**
     * Shows the head of the buffer. This method won't modify the queue.
     * @return next in order frame input, or null if not present.
     */
    @Override
    public FrameInput head()
    {
        Entry<Integer, Serializable> firstFrame = commandBuffer.firstEntry();
        return new FrameInput(firstFrame.getKey(), firstFrame.getValue());
    }
    
    /**
     * Inserts all the inputs passed, provided they're in the interval currently
     * accepted. If a FrameInput it's out of the interval it's discarded. 
     * @param inputs the FrameInputs to insert
     * @return the FrameACK to send back
     */
    @Override
    public FrameACK push(FrameInput[] inputs)
    {
        for(FrameInput input : inputs)
            _push(input);
        
        executionSemaphore.release();
        
        return new FrameACK(lastInOrderACK.get(), _getSelectiveACKs());
    }
        
    /**
     * Inserts the input passed, provided it is in the interval currently
     * accepted. Otherwise it's discarded.
     * 
     * @param input the FrameInput to insert
     * @return the FrameACK to send back
     */
    @Override
    public FrameACK push(FrameInput<Command> input)
    {
        _push(input);
        
        executionSemaphore.release(); //let sem know there is something new
        return new FrameACK(lastInOrderACK.get(), _getSelectiveACKs());
    }
    
    /**
     * Inserts the signal passed, provided it's not a duplicate.
     * 
     * @param signal the FrameInput to insert
     * @return the FrameACK to send back
     */
    @Override
    public FrameACK pushDisconnectionSignal(FrameInput<DisconnectionSignal> signal)
    {
        _push(signal);
        
        return new FrameACK(lastInOrderACK.get(), _getSelectiveACKs());
    }
        
    /**
     * Internal method to push a single input into the queue.
     * 
     * @param input the input to push into the queue
     * @return A boolean indicating whether the input should be selectively ACKed
     */
    private void _push(FrameInput input)
    {
        if(input.getFrameNumber() > lastInOrderACK.get() && !selectiveACKsSet.contains(input.getFrameNumber())) 
        {
            commandBuffer.putIfAbsent(input.getFrameNumber(), input.getCommand());
            if(input.getFrameNumber() == this.lastInOrderACK.get() + 1)
            {
                lastInOrderACK.incrementAndGet();

                while(!this.selectiveACKsSet.isEmpty() && this.selectiveACKsSet.first() == (this.lastInOrderACK.get() + 1))
                {
                    this.lastInOrderACK.incrementAndGet();
                    this.selectiveACKsSet.removeAll(this.selectiveACKsSet.headSet(lastInOrderACK.get(), true));
                }
            }
            else
            {
                this.selectiveACKsSet.add(input.getFrameNumber());
            }
        }
        else
        {
            LOG.debug("Duplicate frame arrived");
        }        
    }
        
     /**
     * Extract an int array containing the selective ACKs
     * 
     * @return the int array of selective ACKs
     */
    private int[] _getSelectiveACKs()
    {
        Integer[] selectiveACKsIntegerArray = this.selectiveACKsSet.toArray(new Integer[0]);
        if(selectiveACKsIntegerArray.length > 0)
        {
            int[] selectiveACKs = ArrayUtils.toPrimitive(selectiveACKsIntegerArray);
            return selectiveACKs;
        }
        else
            return null;
    }
    
    @Override
    public String toString()
    {
        String string = new String();
        
        string += "ExecutionFrameQueue[" + senderID + "] = {";
        for(Map.Entry<Integer, Serializable> entry : this.commandBuffer.entrySet())
        {
            string += " " + entry.getKey();
        }
                
        return string;
    }
    
}
