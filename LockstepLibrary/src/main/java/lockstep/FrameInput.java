/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lockstep;

import java.io.Serializable;

/**
 *
 * @author Raff
 * @param <Command> user defined class. Must extend serializable. Represents an 
 * input command for the server.
 */
public class FrameInput<Command extends Serializable> implements Serializable 
{
    int frameNumber;
    Command cmd;
    
    public FrameInput(int frameNumber, Command cmd)
    {
        this.frameNumber = frameNumber;
        this.cmd = cmd;
    }
}
