/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lockstep;

import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import lockstep.messages.simulation.FrameACK;

/**
 *
 * @author enric
 */
public class ACKQueue {
    
    ConcurrentSkipListMap<Integer, FrameACK> ackQueue;
    
    public ACKQueue()
    {
        ackQueue = new ConcurrentSkipListMap<>();
    }
    
    public FrameACK[] getACKs()
    {
        FrameACK[] acks = ackQueue.values().toArray(new FrameACK[0]);
        
        ackQueue.clear();
        
        return acks;
    }
    
    public void pushACKs(FrameACK ack)
    {
        ackQueue.put(ack.senderID, ack);
    }
    
}
