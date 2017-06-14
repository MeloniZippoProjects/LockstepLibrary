/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lockstep.messages.simulation;

import java.io.Serializable;
import lockstep.FrameInput;

public class InputMessage implements Serializable
{
    public final int senderID;
    public final FrameInput frame;
    
    public InputMessage(int senderID, FrameInput frame)
    {
        this.senderID = senderID;
        this.frame = frame;
    }
    
    
    @Override
    public String toString()
    {
        return "[ " + frame + " ]";
    }
}
