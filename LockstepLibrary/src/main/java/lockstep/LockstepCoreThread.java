/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lockstep;

/**
 * Common ancestor for LockstepClient and LockstepServer.
 * Provides a common interface for disconnection handling and termination.
 */
abstract public class LockstepCoreThread extends Thread
{
    /**
     * First step in the disconnection handling.
     * Called by the receiver, it clears transmitting queues which have lost 
     * their recipient.
     * 
     * @param nodeID ID of the disconnected node
     */
    abstract public void disconnectTransmittingQueues(int nodeID);
    
    /**
     * Second step in the disconnection handling.
     * Called internally, it clears a receving queue which has lost its sender
     * and is now empty.
     * 
     * @param nodeID ID of the disconnected node
     */
    abstract void disconnectReceivingQueues(int nodeID);
    
    /**
     * This method will cause the thread to gracefully release all its resources
     * and stop processing.
     */
    abstract public void abort();
}
