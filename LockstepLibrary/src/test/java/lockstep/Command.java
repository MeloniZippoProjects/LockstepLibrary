/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lockstep;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import lockstep.messages.simulation.LockstepCommand;

/**
 *
 * @author enric
 */
class Command implements LockstepCommand
{
    int up_down;
    int right_left;
    
    public Command(int upd, int rl)
    {
        up_down = upd;
        right_left = rl;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
        out.write(up_down);
        out.write(right_left);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        up_down = in.readInt();
        right_left = in.readInt();
    }
}