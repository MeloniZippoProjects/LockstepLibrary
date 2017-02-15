/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lockstep.messages.simulation;

import java.io.Serializable;
import lockstep.FrameInput;

/**
 *
 * @author Raff
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
}
