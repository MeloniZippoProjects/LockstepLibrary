/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lockstep;

abstract public class LockstepCoreThread extends Thread
{
    abstract public void disconnectTransmittingQueues(int nodeID);
    abstract void disconnectReceivingQueues(int nodeID);
    abstract public void abort();
}
