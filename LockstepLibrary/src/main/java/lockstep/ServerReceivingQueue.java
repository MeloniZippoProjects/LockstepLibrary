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
import lockstep.messages.simulation.FrameACK;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.log4j.Logger;

/**
 * This frame queue supports out of order and simultaneous insertion, amd single
 * extraction of the first available frame. 
 * A semaphore is released when a frame input is available.
 * 
 * It is thread safe.
 */

public class ServerReceivingQueue<Command extends Serializable> implements ReceivingQueue {
    

    volatile ConcurrentSkipListMap<Integer, Command> commandBuffer;
    volatile ConcurrentSkipListSet<Integer> selectiveACKsSet;
    volatile AtomicInteger lastInOrder;

    volatile Semaphore executionSemaphore;
    
    private static final Logger LOG = Logger.getLogger(ClientReceivingQueue.class.getName());
    private final int senderID;
    
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
        this.commandBuffer = new ConcurrentSkipListMap<>();
        this.lastInOrder = new AtomicInteger(initialFrameNumber - 1);
        this.selectiveACKsSet = new ConcurrentSkipListSet<>();
        this.senderID = senderID;
        this.executionSemaphore = serverExecutionSemaphore;
        
        System.out.println("BufferHead["+senderID+"] initialized at " + initialFrameNumber);
    }
    
    /**
     * Extracts the first available frame input. 
     * This method will change the queue, extracting the head, only if it's present.
     * 
     * @return the next in order frame input, or null if not present. 
     */
    public FrameInput<Command> pop()
    {
        //Command nextCommand = this.commandBuffer.get(nextFrame.get());
        
        Entry<Integer, Command> firstFrame = commandBuffer.pollFirstEntry();
        
        if( firstFrame != null ) //there's something in Q
        {
            if(commandBuffer.firstEntry() != null) //if there is something else, let the semaphore know
                executionSemaphore.release();
            
            return new FrameInput(firstFrame.getKey(), firstFrame.getValue());
        }
        
        return null;
    }
    
    /**
     * Shows the head of the buffer. This method won't modify the queue.
     * @return next in order frame input, or null if not present.
     */
    public FrameInput<Command> head()
    {
        Entry<Integer,Command> firstFrame = commandBuffer.firstEntry();
        return new FrameInput(firstFrame.getKey(), firstFrame.getValue());
    }
    
    /**
     * Inserts all the inputs passed, provided they're in the interval currently
     * accepted. If a FrameInput it's out of the interval it's discarded. 
     * @param inputs the FrameInputs to insert
     * @return the FrameACK to send back
     */
    public FrameACK push(FrameInput[] inputs)
    {
        for(FrameInput input : inputs)
            _push(input);
        
        executionSemaphore.release();
        
        return new FrameACK(lastInOrder.get(), _getSelectiveACKs());
    }
        
    /**
     * Inserts the input passed, provided it is in the interval currently
     * accepted. Otherwise it's discarded.
     * 
     * @param input the FrameInput to insert
     * @return the FrameACK to send back
     */
    public FrameACK push(FrameInput input)
    {
        _push(input);
        
        executionSemaphore.release(); //let sem know there is something new
        return new FrameACK(lastInOrder.get(), _getSelectiveACKs());
    }
        
    /**
     * Internal method to push a single input into the queue.
     * 
     * @param input the input to push into the queue
     * @return A boolean indicating whether the input should be selectively ACKed
     */
    private void _push(FrameInput<Command> input)
    {
        try
        {
            if( input.getFrameNumber() > lastInOrder.get() && this.commandBuffer.putIfAbsent(input.getFrameNumber(), input.getCommand()) == null)
            {
                if(input.getFrameNumber() == this.lastInOrder.get() + 1)
                {
                    lastInOrder.incrementAndGet();
                    
                    //System.out.println("["+senderID+"]first: " + (selectiveACKsSet.isEmpty() ? "vuoto" : selectiveACKsSet.first() ) + "; lastinorder: " + (lastInOrder.get() + 1));

                    //System.out.println("["+senderID+"] lastInOrder prima: " + lastInOrder.get());
                    while(!this.selectiveACKsSet.isEmpty() && this.selectiveACKsSet.first() == (this.lastInOrder.get() + 1))
                    {
                        //System.out.println("Entrato");
                        this.lastInOrder.incrementAndGet();
                        this.selectiveACKsSet.removeAll(this.selectiveACKsSet.headSet(lastInOrder.get(), true));
                    }
                    //System.out.println("["+senderID+"]lastInOrder dopo: " + lastInOrder.get());
                    /*
                    if(input.getFrameNumber() == this.nextFrame.get())
                    {
                        //LOG.debug("Countdown to " + ( executionSemaphore.getCount() - 1) + " made by " + senderID);
                        executionSemaphore.release();
                    }
                    */
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
        catch(NullPointerException e)
        {
            LOG.debug("SEGFAULT for " + senderID);
            e.printStackTrace();
            System.exit(1);
        }
    }
        
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
        for(Map.Entry<Integer, Command> entry : this.commandBuffer.entrySet())
        {
            string += " " + entry.getKey();
        }
        //string += " } nextFrame = " + nextFrame.get() + " lastInOrder " + lastInOrder.get();
                
        return string;
    }
    
}
