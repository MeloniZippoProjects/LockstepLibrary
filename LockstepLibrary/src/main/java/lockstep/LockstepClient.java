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
public abstract class LockstepClient implements Runnable
{
    int interframeTime;
    ExecutionFrameQueue[] frameQueues;    

    /**
     * Used for synchronization between server and executionFrameQueues
     */
    Map<Integer, Boolean> executionQueuesHeadsAvailability;

    public LockstepClient()
    {
        //Inizializzazione campi...
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
                synchronized(executionQueuesHeadsAvailability)
                {
                    if(executionQueuesHeadsAvailability.contains(Boolean.FALSE))
                    {
                        suspendSimulation();
                        while(executionQueuesHeadsAvailability.contains(Boolean.FALSE))
                            wait();
                        resumeSimulation();
                    }
                }  

                FrameInput[] inputs = collectInputs();
                for(FrameInput input : inputs)
                    executeFrameInput(input);
                
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
                idx++;
            }
        }        
        return inputs;
    }
}