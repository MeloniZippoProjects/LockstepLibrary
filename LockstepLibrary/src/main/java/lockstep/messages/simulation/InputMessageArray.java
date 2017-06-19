/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lockstep.messages.simulation;

import java.io.Serializable;
import lockstep.FrameInput;

/**
 * Wrapper for an array of FrameInputs and the id of the sender
 */
public class InputMessageArray implements Serializable
{
    public final int senderID;
    public final FrameInput[] frames;
    
    public InputMessageArray(int hostID, FrameInput[] frames)
    {
        this.senderID = hostID;
        this.frames = frames;
    }
    
    @Override
    public String toString()
    {
        String str = "[ ";
        
        for(int i = 0; i < frames.length; ++i)
            str += "" + frames[i] + " ";
        
        str += "]";
        return str;
    }
}
