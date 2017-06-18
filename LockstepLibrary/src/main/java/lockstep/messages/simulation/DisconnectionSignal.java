/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lockstep.messages.simulation;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Command indicating that the sending client disconnected from the simulation.
 */
public class DisconnectionSignal implements LockstepCommand, Externalizable
{
    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
    }
}
