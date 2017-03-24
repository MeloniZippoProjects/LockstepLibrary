/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lockstep;

import java.io.Serializable;
import java.util.Objects;

/**
 *
 * @author Raff
 * @param <Command> user defined class. Must extend serializable. Represents an 
 * input command for the server.
 */
public class FrameInput<Command extends Serializable> implements Serializable 
{
    private final int frameNumber;
    private final Command cmd;
    
    public FrameInput(int frameNumber, Command cmd)
    {
        this.frameNumber = frameNumber;
        this.cmd = cmd;
    }

    public int getFrameNumber() {
        return frameNumber;
    }

    public Command getCommand() {
        return cmd;
    }    

    @Override
    public int hashCode()
    {
        int hash = 5;
        return hash;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        final FrameInput<?> other = (FrameInput<?>) obj;
        if (this.frameNumber != other.frameNumber)
        {
            return false;
        }
        return true;
    }   
    
    @Override
    public String toString()
    {
        return "" + frameNumber;
    }
}
