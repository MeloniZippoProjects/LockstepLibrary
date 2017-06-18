/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lockstep;

import java.util.concurrent.ConcurrentSkipListMap;
import lockstep.messages.simulation.FrameACK;

/**
 * Used to store ACK to be sent back.
 * ACKs are inserted by the receiver and retrieved by the transmitter.
 * 
 * It is thread safe.
 */
public class ACKSet {
      ConcurrentSkipListMap<Integer, FrameACK> ackMap;
    
    public ACKSet()
    {
        ackMap = new ConcurrentSkipListMap<>();
    }
    
    public FrameACK[] getACKs()
    {
        FrameACK[] acks = ackMap.values().toArray(new FrameACK[0]);
        
        ackMap.clear();
        
        return acks;
    }
    
    public void pushACK(FrameACK ack)
    {
        ackMap.put(ack.senderID, ack);
    }
}
