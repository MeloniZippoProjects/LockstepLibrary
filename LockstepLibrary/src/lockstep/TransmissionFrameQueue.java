/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lockstep;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;
import lockstep.messages.FrameACK;

/**
 * This frame queue supports out of order insertions, while extractions get the 
 * whole queue. pop() does not remove items, as they are removed only after the
 * relative ACK is received.
 * <p>
 * It's thread safe, as producer and consumer access different locations of this
 * data structure.
 * @author Raff
 */
public class TransmissionFrameQueue
{
    /**
     * Internally it behaves as an infinite array of which, at any time, only
     * indexes in [baseFrameNumber, baseFrameNumber + bufferSize - 1] can be
     * accessed.
     */
    
    int lastACKed;
    Map<Integer, FrameInput> frameBuffer;
    
    /**
     * 
     * @param bufferSize Size of the internal buffer. It's important to
     * dimension this large enough to store the received frames without forcing
     * retransmissions
     * @param initialFrameNumber First frame's number. Must be the same for all 
     * the clients using the protocol
     */
    public TransmissionFrameQueue(int bufferSize, int initialFrameNumber)
    {
        this.frameBuffer = new ConcurrentSkipListMap<Integer, FrameInput>();
        this.lastACKed = initialFrameNumber - 1;
    }
    
    /**
     * Inserts the input passed, provided it is in the interval currently
     * accepted. Otherwise it's discarded.
     * 
     * @param input the FrameInput to insert
     */
    public void push(FrameInput input)
    {
        if(input.frameNumber >= this.lastACKed && !this.frameBuffer.containsKey(input.frameNumber))
        {
            this.frameBuffer.put(input.frameNumber, input);
        }
    }
    
    /**
     * Extracts the all frame inputs to send. This method is not destructive,
     * as items are removed only after the relative ACK is received.
     * 
     * @return an array containing the frame input to send
     */
    public FrameInput[] pop()
    {
        Set<Entry<Integer, FrameInput>> entries = this.frameBuffer.entrySet();
        FrameInput[] toRet = new FrameInput[entries.size()];
        int i = 0;
        
        for(Entry<Integer, FrameInput> entry : entries)
        {
            toRet[i++] = entry.getValue();
        }
        
        return toRet;
    }
    
    /**
     * Process the received ACKwnoledgement to remove packets successfully
     * delivered from the transmitting queue.
     * 
     * @param ack the ACKwnoledgement received
     */
    public void processACK(FrameACK ack)
    {
        if(ack.cumulativeACK > this.lastACKed)
        {
            for(int frameNumber = this.lastACKed + 1; frameNumber <= ack.cumulativeACK; frameNumber++)
            {
                this.frameBuffer.remove(frameNumber);
            }
            this.lastACKed = ack.cumulativeACK;
        }
        
        for(int frameNumber : ack.selectiveACKs)
        {
            this.frameBuffer.remove(frameNumber);
        }
    }
}
