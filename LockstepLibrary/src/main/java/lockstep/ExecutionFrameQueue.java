/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lockstep;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;
import lockstep.messages.simulation.FrameACK;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.log4j.Logger;


/**
 * This frame queue supports out of order and simultaneous insertion, but only 
 * in order single extraction.
 *
 * It is thread safe, as producer and consumer access different locations of this
 * data structure.
 * 
 * @author Raff
 */

class ExecutionFrameQueue<Command extends Serializable>
{
    /**
     * Internally it behaves as an infinite array of which, at any time, only
     * indexes in [baseFrameNumber, baseFrameNumber + bufferSize - 1] can be
     * accessed.
     */
    
    int nextFrame;
    //int baseFrameNumber;
    //FrameInput[] frameBuffer;

    ConcurrentSkipListMap<Integer, Command> commandBuffer;
    ConcurrentSkipListSet<Integer> selectiveACKsSet;
    AtomicInteger lastInOrder;

    CyclicCountDownLatch cyclicExecutionLatch;
    
    private static final Logger LOG = Logger.getLogger(ExecutionFrameQueue.class.getName());
    private final int hostID;
    
    /**
     * Creates a new ExecutionFrameQueue
     * @param bufferSize Size of the internal buffer. It's important to
     * dimension this large enough to store the received frames without forcing
     * retransmissions
     * @param initialFrameNumber First frame's number. Must be the same for all 
     * the clients using the protocol
     */
    public ExecutionFrameQueue(int initialFrameNumber, int hostID, CyclicCountDownLatch cyclicExecutionLatch)
    {
        this.commandBuffer = new ConcurrentSkipListMap<>();
        this.nextFrame = initialFrameNumber;
        this.lastInOrder = new AtomicInteger(initialFrameNumber - 1);
        this.selectiveACKsSet = new ConcurrentSkipListSet<>();
        this.hostID = hostID;
        this.cyclicExecutionLatch = cyclicExecutionLatch;
        
        LOG.debug("BufferHead initialized at " + initialFrameNumber);
    }
    
    /**
     * Extracts the next frame input only if it's in order. 
     * This method will change the queue, extracting the head, only if it's present.
     * @return the next in order frame input, or null if not present. 
     */
    public Command pop()
    {
        Command nextCommand = this.commandBuffer.get(nextFrame);
        if( nextCommand != null )
        {
            nextFrame++;
            for(Integer key : commandBuffer.headMap(nextFrame).keySet())
                commandBuffer.remove(key);
            
            if(commandBuffer.get(nextFrame) != null)
            {
                LOG.debug("Countdown to " + ( cyclicExecutionLatch.getCount() - 1) + "made by " + hostID);
                cyclicExecutionLatch.countDown();          
            }
        }
        else
        {
            LOG.debug("ExecutionFrameQueue " + hostID + ": FrameInput missing for current frame");
        }
        return nextCommand;
    }
    
    /**
     * Shows the head of the buffer. This method won't modify the queue.
     * @return next in order frame input, or null if not present.
     */
    public FrameInput head()
    {
        return new FrameInput(nextFrame, commandBuffer.get(nextFrame));
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
            if( this.commandBuffer.putIfAbsent(input.getFrameNumber(), input.getCommand()) == null)
            {
                if(input.getFrameNumber() == this.lastInOrder.get() + 1)
                {
                    lastInOrder.incrementAndGet();
                    while(!this.selectiveACKsSet.isEmpty() && this.selectiveACKsSet.first() == this.lastInOrder.get() + 1)
                    {
                        this.lastInOrder.incrementAndGet();
                        this.selectiveACKsSet.remove(this.selectiveACKsSet.first());
                    }

                    if(input.getFrameNumber() == this.nextFrame)
                    {
                        LOG.debug("Countdown to " + ( cyclicExecutionLatch.getCount() - 1) + " made by " + hostID);
                        cyclicExecutionLatch.countDown();
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
        catch(NullPointerException e)
        {
            LOG.debug("SEGFAULT for " + hostID);
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
        
        string += "ExecutionFrameQueue[" + hostID + "] = {";
        for(Entry<Integer, Command> entry : this.commandBuffer.entrySet())
        {
            string += " " + entry.getKey();
        }
        string += " } nextFrame = " + nextFrame + " lastInOrder " + lastInOrder.get();
                
        return string;
    }
}
