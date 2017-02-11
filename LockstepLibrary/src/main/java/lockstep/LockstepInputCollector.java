/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lockstep;

import java.io.Serializable;

/**
 * To be implemented: collects application inputs during frames and transforms
 * them in FrameInput at the end of the frame, then sends it to the other objects. * 
 * 
 * @param <Command>
 */
public abstract class LockstepInputCollector<Command extends Serializable> implements Runnable
{
    int currentFrame;
    int interFrameTime;
    
    //execution queue for the local queue
    ExecutionFrameQueue executionFrameQueue;
    
    //transmission queue for the remote clients or server
    TransmissionFrameQueue[] transmissionFrameQueues;
        
    /**
     * Must be defined. It has to read input from user, and return a Command 
     * object to be executed. If there is no input in a frame a Command object
     * must still be returned, possibly representing the lack of an user input
     * in the semantic of the application
     * 
     * @return the Command object collected in the current frame
     */
    protected abstract Command readInput();
    
    @Override
    public void run()
    {
        while(true)
        {
            try
            {
                //it's never null
                Command cmd = readInput(); 
                
                executionFrameQueue.push(new FrameInput(currentFrame, cmd));
                for(TransmissionFrameQueue transmissionQueue : transmissionFrameQueues)
                {
                    transmissionQueue.push(new FrameInput(currentFrame, cmd));
                }
                
                Thread.sleep(interFrameTime);
            }
            catch(InterruptedException e)
            {
                e.printStackTrace();
            }           
        }
    }
    
}
