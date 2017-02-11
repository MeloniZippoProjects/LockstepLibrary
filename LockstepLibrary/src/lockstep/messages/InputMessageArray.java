/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lockstep.messages;

import lockstep.FrameInput;

/**
 *
 * @author Raff
 */
public class InputMessageArray
{
    public final int hostID;
    public final FrameInput[] frames;
    
    public InputMessage(int hostID, FrameInput[] frames)
    {
        this.hostID = hostID;
        this.frames = frames;
    }
}
