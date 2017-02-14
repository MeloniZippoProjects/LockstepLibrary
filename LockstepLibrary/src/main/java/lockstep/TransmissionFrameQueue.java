/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lockstep;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import lockstep.messages.simulation.FrameACK;
import org.apache.log4j.Logger;

/**
 * This frame queue supports out of order insertions, while extractions get the 
 * whole queue. pop() does not remove items, as they are removed only after the
 * relative ACK is received.
 *
 * It's thread safe, as producer and consumer access different locations of this
 * data structure.
 * @author Raff
 */
public class TransmissionFrameQueue<Command extends Serializable>
{
    /**
     * Internally it behaves as an infinite array of which, at any time, only
     * indexes in [baseFrameNumber, baseFrameNumber + bufferSize - 1] can be
     * accessed.
     */
    
    int lastFrame;
    AtomicInteger lastACKed;
    ConcurrentSkipListMap<Integer, Command> commandsBuffer;
    
    Semaphore transmissionSemaphore;
    
    private static final Logger LOG = Logger.getLogger(TransmissionFrameQueue.class.getName());
    private final int hostID;

    /**
     * 
     * @param transmissionSemaphore
     * @param initialFrameNumber First frame's number. Must be the same for all 
     * the clients using the protocol
     */
    public TransmissionFrameQueue(int initialFrameNumber, Semaphore transmissionSemaphore, int hostID)
    {
        this.lastFrame = initialFrameNumber - 1;
        this.commandsBuffer = new ConcurrentSkipListMap<>();
        this.lastACKed = new AtomicInteger(initialFrameNumber - 1);
        this.transmissionSemaphore = transmissionSemaphore;
        this.hostID = hostID;
    }
    
    /**
     * Inserts the input passed, provided it is in the interval currently
     * accepted. Otherwise it's discarded.
     * 
     * @param input the FrameInput to insert
     */
    public void push(Command command)
    {
        commandsBuffer.put(lastFrame + 1, command);
        this.transmissionSemaphore.release();
        LOG.debug("Released a permit for semaphore[" + hostID + "], current permits: " + transmissionSemaphore.availablePermits());
    }
    
    /**
     * Inserts all inputs passed, provided it is in the interval currently 
     * accepted. Otherwise it is discarded.
     * @param inputs array of inputs to be transmitted
     */
    public void push(Command[] commands)
    {
        for(Command command : commands)
            push(command);
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
    public FrameInput<Command>[] pop()
    {
        Set<Entry<Integer, Command>> commandEntries = commandsBuffer.entrySet();
        ArrayList<FrameInput<Command>> toRet = new ArrayList<>();        
        for (Entry<Integer, Command> commandEntry : commandEntries) {
            toRet.add(new FrameInput<Command>(commandEntry.getKey(), commandEntry.getValue()));
        }
        return (FrameInput[]) toRet.toArray(new FrameInput[0]);
    }
    
    /**
     * Process the received ACKwnoledgement to remove packets successfully
     * delivered from the transmitting queue.
     *
     * Calls to this method may conflict with concurrent calls to pop().
     * However, we assume that sending innecessary frames creates no issues
     * and has acceptable cost vs synchronization overhead.
     * 
     * @param ack the ACKwnoledgement received
     */
    public void processACK(FrameACK ack)
    {
        int acked = 0;
        
        if(ack.cumulativeACK > this.lastACKed.get())
        {
            for(int frameNumber = this.lastACKed.get() + 1; frameNumber <= ack.cumulativeACK; frameNumber++)
            {
                commandsBuffer.remove(frameNumber);
                acked++;
            }
            this.lastACKed.set(ack.cumulativeACK);
        }
        
        if(ack.selectiveACKs != null)
        {
            for(int frameNumber : ack.selectiveACKs)
            {
                commandsBuffer.remove(frameNumber);
                acked++;
            }
        }
        
        transmissionSemaphore.release(commandsBuffer.size());
        
        LOG.debug("" + acked + " ACKs received");
    }
}
