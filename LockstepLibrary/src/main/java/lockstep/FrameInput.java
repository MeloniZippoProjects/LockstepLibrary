/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lockstep;

import java.io.Serializable;
import lockstep.messages.simulation.LockstepCommand;

public class FrameInput implements Serializable 
{
    private final int frameNumber;
    private final LockstepCommand cmd;
    
    public FrameInput(int frameNumber, LockstepCommand cmd)
    {
        this.frameNumber = frameNumber;
        this.cmd = cmd;
    }

    public int getFrameNumber() {
        return frameNumber;
    }

    public LockstepCommand getCommand() {
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
        
        final FrameInput other = (FrameInput) obj;
        return this.frameNumber == other.frameNumber;
    }   
    
    @Override
    public String toString()
    {
        return "" + frameNumber;
    }
}
