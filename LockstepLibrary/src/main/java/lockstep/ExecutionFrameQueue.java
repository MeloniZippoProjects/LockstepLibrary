/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lockstep;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListSet;
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

class ExecutionFrameQueue
{
    /**
     * Internally it behaves as an infinite array of which, at any time, only
     * indexes in [baseFrameNumber, baseFrameNumber + bufferSize - 1] can be
     * accessed.
     */
    
    int bufferSize;
    int bufferHead;
    //int baseFrameNumber;
    //FrameInput[] frameBuffer;

    Map<Integer, FrameInput> frameBuffer;
    
    int lastInOrder;
    ConcurrentSkipListSet<Integer> selectiveACKsSet;

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
        this.frameBuffer = new HashMap<Integer, FrameInput>();
        this.bufferHead = initialFrameNumber;
        this.lastInOrder = initialFrameNumber - 1;
        this.selectiveACKsSet = new ConcurrentSkipListSet<>();
        this.hostID = hostID;
        this.cyclicExecutionLatch = cyclicExecutionLatch;
    }
    
    /**
     * Extracts the next frame input only if it's in order. 
     * This method will change the queue, extracting the head, only if it's present.
     * @return the next in order frame input, or null if not present. 
     */
    public FrameInput pop()
    {
        FrameInput nextInput = this.frameBuffer.get(bufferHead);
        if( nextInput != null )
        {
            this.frameBuffer.remove(bufferHead);
            this.bufferHead++;
            if(frameBuffer.get(bufferHead) != null)
            {
                LOG.debug("Coundown to " + ( cyclicExecutionLatch.getCount() - 1));
                cyclicExecutionLatch.countDown();          
            }
        }
        else
        {
            LOG.debug("ExecutionFrameQueue " + hostID + ": FrameInput missing for current frame");
        }
        return nextInput;
    }
    
    /**
     * Shows the head of the buffer. This method won't modify the queue.
     * @return next in order frame input, or null if not present.
     */
    public FrameInput head()
    {
        return this.frameBuffer.get(bufferHead);
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
        {
            boolean toSelectivelyACK = _push(input);
            if(toSelectivelyACK)
                selectiveACKsSet.add(input.frameNumber);
        }
        
        return new FrameACK(lastInOrder, _getSelectiveACKs());
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
        boolean toSelectivelyACK = _push(input);
        if(toSelectivelyACK)
            this.selectiveACKsSet.add(input.frameNumber);
        
        return new FrameACK(lastInOrder, _getSelectiveACKs());
    }
        
    /**
     * Internal method to push a single input into the queue.
     * 
     * @param input the input to push into the queue
     * @return A boolean indicating whether the input should be selectively ACKed
     */
    private boolean _push(FrameInput input)
    {
        if(input.frameNumber >= this.bufferHead)
        {
            if( this.frameBuffer.putIfAbsent(input.frameNumber, input) == null)
            {
                if(input.frameNumber == this.bufferHead)
                {
                    LOG.debug("Coundown to " + (cyclicExecutionLatch.getCount() - 1));
                    cyclicExecutionLatch.countDown();
                }

                if(input.frameNumber == this.lastInOrder + 1)
                {                
                    lastInOrder++;
                    while(!this.selectiveACKsSet.isEmpty() && this.selectiveACKsSet.first() == this.lastInOrder + 1)
                    {
                        this.lastInOrder++;
                        this.selectiveACKsSet.remove(this.selectiveACKsSet.first());
                    }
                    return false;
                }
                else
                    return true;
            }
            else
                return false;
        }
        else
        {
            LOG.debug("Frame arrived out of buffer bound");
            return false;
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
}
