/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lockstep;

import java.util.ArrayList;
import lockstep.messages.FrameACK;

/**
 * This frame queue supports out of order and simultaneous insertion, but only 
 * in order single extraction.
 *
 * It's thread safe, as producer and consumer access different locations of this
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
    int baseFrameNumber;
    int lastInOrder;
    FrameInput[] frameBuffer;
    
    /**
     * 
     * @param bufferSize Size of the internal buffer. It's important to
     * dimension this large enough to store the received frames without forcing
     * retransmissions
     * @param initialFrameNumber First frame's number. Must be the same for all 
     * the clients using the protocol
     */
    public ExecutionFrameQueue(int bufferSize, int initialFrameNumber)
    {
        this.bufferSize = bufferSize;
        this.frameBuffer = new FrameInput[bufferSize];
        this.bufferHead = 0;
        this.baseFrameNumber = initialFrameNumber;
        this.lastInOrder = initialFrameNumber - 1;
    }
    
    /**
     * Inserts all the inputs passed, provided they're in the interval currently
     * accepted. If a FrameInput it's out of the interval it's discarded. 
     * 
     * @param inputs the FrameInputs to insert
     * @return the number of the most recent in order input. Used for ACKs
     */
    public FrameACK push(FrameInput[] inputs)
    {
        ArrayList<Integer> selectiveAckList = new ArrayList<Integer>();
        
        for(FrameInput input : inputs)
        {
            int acked = put(input);
            if(acked != -1)
                selectiveAckList.add(acked);
        }   
        
        int[] selectiveACKs = new int[selectiveAckList.size()];
        
        int i = 0;
        for(int ack : selectiveAckList)
        {
            selectiveACKs[i] = ack;
            ++i;
        }
        
        return new FrameACK(lastInOrder, selectiveACKs);
    }
    
    
    /**
     * Inserts the input passed, provided it is in the interval currently
     * accepted. Otherwise it's discarded.
     * 
     * @param input the FrameInput to insert
     * @return the number of the most recent in order input. Used for ACKs
     */
    public FrameACK push(FrameInput input)
    {
        int ack = put(input);
        
        int[] selectiveAck;
        
        if(ack != -1)
        {
            selectiveAck = new int[1];
            selectiveAck[0] = ack;
        }
        else
        {
            selectiveAck = new int[0];
        }
        
        return new FrameACK(lastInOrder, selectiveAck);
    }
    
    
    
    private int put(FrameInput input)
    {
        int toAck = -1;
        
        if(input.frameNumber >= this.baseFrameNumber && input.frameNumber <= this.baseFrameNumber + this.bufferSize -1)
        {
            int bufferIndex = (input.frameNumber - this.baseFrameNumber + this.bufferHead) % this.bufferSize;
            if(this.frameBuffer[bufferIndex] == null)
                this.frameBuffer[bufferIndex] = input;
            
            if(input.frameNumber == this.lastInOrder + 1)
                this.lastInOrder++;
            else
                toAck = input.frameNumber;
            
        }
        
        return toAck;
    }
    
    /**
     * Extracts the next frame input only if it's in order. 
     * @return the next in order frame input, or null if not present
     */
    public FrameInput pop()
    {
        FrameInput nextInput = this.frameBuffer[this.bufferHead];
        if( nextInput != null)
        {
            this.frameBuffer[this.bufferHead] = null;
            this.bufferHead = (this.bufferHead + 1) % this.bufferSize;
        }
        return nextInput;
    }
}
