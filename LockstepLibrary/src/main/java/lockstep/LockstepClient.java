/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lockstep;

import java.io.Serializable;
import java.net.Socket;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Raff
 */
public abstract class LockstepClient<Command extends Serializable> implements Runnable
{
    int currentFrame;
    int interframeTime;
    int hostID;
    Map<Integer, ExecutionFrameQueue> executionFrameQueues; 
    TransmissionFrameQueue transmissionFrameQueue;
    
    /**
     * Used for synchronization between server and executionFrameQueues
     */
    Map<Integer, Boolean> executionQueuesHeadsAvailability;

    public LockstepClient()
    {
        //Initialization
    }

    /**
     * Must read input from user, and return a Command object to be executed.
     * If there is no input in a frame a Command object must still be returned,
     * possibly representing the lack of an user input in the semantic of the application
     * 
     * @return the Command object collected in the current frame
     */
    protected abstract Command readInput();


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
                readUserInput();
                executeInputs();
                currentFrame++;
                Thread.sleep(interframeTime);
            }
            catch(InterruptedException e)
            {
                e.printStackTrace();
            }           
        }
    }
    
    private void readUserInput()
    {
        Command cmd = readInput();
        executionFrameQueues.get(this.hostID).push(new FrameInput(currentFrame, cmd));
        transmissionFrameQueue.push(new FrameInput(currentFrame, cmd));
    }
    
    private void executeInputs() throws InterruptedException
    {
        if(executionQueuesHeadsAvailability.containsValue(Boolean.FALSE))
        {
            suspendSimulation();
            for(Integer key : executionQueuesHeadsAvailability.keySet())
            {
                if(key != this.hostID)
                {
                    Boolean nextQueueHeadAvailability = executionQueuesHeadsAvailability.get(key);
                    synchronized(nextQueueHeadAvailability)
                    {
                        while(nextQueueHeadAvailability == Boolean.FALSE)
                        {
                            nextQueueHeadAvailability.wait();
                        }
                    }
                }
            }
            resumeSimulation();
        }
                
        FrameInput[] inputs = collectInputs();
        for(FrameInput input : inputs)
            executeFrameInput(input);
    }

    private FrameInput[] collectInputs()
    {
        FrameInput[] inputs = new FrameInput[this.executionFrameQueues.size()];
        int idx = 0;
        for(ExecutionFrameQueue frameQueue : this.executionFrameQueues.values())
        {
            inputs[idx] = frameQueue.pop();
            ++idx;
        }        
        return inputs;
    }
}
