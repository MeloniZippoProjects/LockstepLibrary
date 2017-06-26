/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lockstep;

import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;
import lockstep.messages.simulation.FrameACK;
import lockstep.messages.simulation.LockstepCommand;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * This frame queue supports out of order insertions, while extractions gets the 
 * whole queue. pop() does not remove items, as they are removed only after the
 * relative ACK is received.
 *
 * It is thread safe.
 */

public class TransmissionQueue
{    
    ConcurrentSkipListMap<Integer, LockstepCommand> commandsBuffer;
    AtomicInteger lastFrame;
    AtomicInteger lastACKed;
        
    private static final Logger LOG = LogManager.getLogger(TransmissionQueue.class);
    private final int senderID;

    /**
     * Constructor.
     * @param initialFrameNumber First frame's number. Must be the same for all 
     * the hosts using the protocol
     * @param senderID ID of the client whose frames are collected by this queue
     */
    public TransmissionQueue(int initialFrameNumber, int senderID)
    {
        this.lastFrame = new AtomicInteger(initialFrameNumber - 1);
        this.commandsBuffer = new ConcurrentSkipListMap<>();
        this.lastACKed = new AtomicInteger(initialFrameNumber - 1);
        this.senderID = senderID;
    }
    
    /**
     * Inserts the input passed, provided it is in the interval currently
     * accepted. Otherwise it's discarded.
     * 
     * @param frameInput the FrameInput to be transmitted
     */
    public void push(FrameInput frameInput)
    {
        commandsBuffer.putIfAbsent(frameInput.getFrameNumber(), frameInput.getCommand());
    }
    
    /**
     * Inserts all inputs passed, provided it is in the interval currently 
     * accepted. Otherwise it is discarded.
     * @param frameInputs array of inputs to be transmitted
     */
    public void push(FrameInput[] frameInputs)
    {
        for(FrameInput frameInput : frameInputs)
            push(frameInput);
    }
    
    public boolean hasFramesToSend()
    {
        return !this.commandsBuffer.isEmpty();
    }
    
    /**
     * Extracts the all frame inputs to send. This method is not destructive,
     * as items are removed only after the relative ACK is received.
     * 
     * Calls to this method may conflict with concurrent calls to processACK().
     * However, we assume that sending innecessary frames creates no issues
     * and has acceptable cost vs synchronization overhead.
     * 
     * @return an array containing the frame input to send
     */
    public FrameInput[] pop()
    {
        Set<Entry<Integer, LockstepCommand>> commandEntries = commandsBuffer.entrySet();
        ArrayList<FrameInput> toRet = new ArrayList<>();        
        for (Entry<Integer, LockstepCommand> commandEntry : commandEntries) {
            int frameNumber = commandEntry.getKey();
            LockstepCommand command = commandEntry.getValue();
            toRet.add(new FrameInput(frameNumber, command));
        }
        
        return toRet.toArray(new FrameInput[0]);
    }
    
    /**
     * Process the received ACKwnoledgement to remove packets successfully
     * delivered from the transmitting queue.
     *
     * Calls to this method may conflict with concurrent calls to pop().
     * However, we assume that sending unnecessary frames creates no issues
     * and has acceptable cost vs synchronization overhead.
     * 
     * @param ack the ACKwnoledgement received
     */
    public void processACK(FrameACK ack)
    {
        for(Integer key : commandsBuffer.headMap(ack.cumulativeACK, true).keySet())
            commandsBuffer.remove(key);
        
        lastACKed.set(ack.cumulativeACK);
        
        if(ack.selectiveACKs != null)
            for(int frameNumber : ack.selectiveACKs)
                commandsBuffer.remove(frameNumber);
    }
    
    @Override
    public String toString()
    {
        String string = new String();
        
        string += "TransmissionFrameQueue[" + senderID + "] = {";
        for(Entry<Integer, LockstepCommand> entry : this.commandsBuffer.entrySet())
        {
            string += " " + entry.getKey();
        }
        string += " }; lastAcked = " + lastACKed.get();
                
        return string;
    }
}
