/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lockstep;

/**
 *
 * @author Raff
 */
public abstract class LockstepClient
{
    // Methods left to be implemented by an application-specific subclass
    protected abstract void suspendSimulation();
    protected abstract void resumeSimulation();
    protected abstract void executeFrame(Frame f);
    
    
}
