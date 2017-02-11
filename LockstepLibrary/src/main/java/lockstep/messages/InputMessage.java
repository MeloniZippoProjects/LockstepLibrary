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
public class InputMessage
{
    public final int hostID;
    public final FrameInput frame;
    
    public InputMessage(int hostID, FrameInput frame)
    {
        this.hostID = hostID;
        this.frame = frame;
    }
}
