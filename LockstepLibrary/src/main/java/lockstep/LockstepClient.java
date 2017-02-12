/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lockstep;

import java.net.Socket;
import java.util.Map;

/**
 *
 * @author Raff
 */
public abstract class LockstepClient implements Runnable
{
    int interframeTime;
    ExecutionFrameQueue[] frameQueues;    

    /**
     * Used for synchronization between server and executionFrameQueues
     */
    Map<Integer, Boolean> executionQueuesHeadsAvailability;

    //Input collector...
    
    /**
     * 
     * @param socket socket to the LockstepServer
     */
    public LockstepClient(Socket socket)
    {
        
    }

    /**
     * Must suspend the simulation execution due to a synchronization issue
     */
    protected abstract void suspendSimulation();
    
    /**
     * Must resume the simulation execution, as input are now synchronized
     */
    protected abstract void resumeSimulation();
    
    /**
     * Must get the command contained in the frame input and execute it
     * 
     * @param f the frame input containing the command to execute
     */
    protected abstract void executeFrameInput(FrameInput f);

    @Override
    public void run()
    {
        while(true)
        {
            try
            {
                synchronized(executionQueuesHeadsAvailability)
                {
                    while(executionQueuesHeadsAvailability.containsValue(Boolean.FALSE))
                    {
                        suspendSimulation();
                        //synchronized(inputCollectionMonitor)
                        wait();
                    }
                }  


                FrameInput[] inputs = collectInputs();
                if(inputs == null)
                {
                    suspendSimulation();
                    //Suspend input collection...
                    waitResync();
                    //Resume input collection...
                    resumeSimulation();
                }
                else
                {
                    for(FrameInput input : inputs)
                        executeFrameInput(input);
                }
                
                Thread.sleep(interframeTime);
            }
            catch(InterruptedException e)
            {
                e.printStackTrace();
            }           
        }
    }

    private FrameInput[] collectInputs()
    {
        FrameInput[] inputs = new FrameInput[this.frameQueues.length];
        int idx = 0;
        for(ExecutionFrameQueue frameQueue : this.frameQueues)
        {
            if(frameQueue.head()== null)
                return null;
            else
            {
                inputs[idx] = frameQueue.pop();
                ++idx;
            }
        }        
        return inputs;
    }

    private void waitResync()
    {
      
    }
}