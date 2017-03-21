/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lockstep;

import java.io.Serializable;
import lockstep.messages.simulation.FrameACK;

/**
 *
 * @author enric
 */
public interface ReceivingQueue<Command extends Serializable> {
    
    public FrameInput<Command> pop();
    
    public FrameInput<Command> head();
    
    public FrameACK push(FrameInput[] inputs);
    
    public FrameACK push(FrameInput input);
}
