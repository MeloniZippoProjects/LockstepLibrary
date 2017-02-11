/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lockstep;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 *
 * @author Raff
 */
public class LockstepServer implements Runnable
{
    /**
     * Used without interframe times. As soon as all inputs for a frame are 
     * available, they're forwarded to all the clients
     */
    Map<Integer, ExecutionFrameQueue> executionFrameQueues;
    
    /**
     * Buffers for frame input to send to clients. 
     * For each client partecipating in the session there's a queue for each of
     * the other clients.
     */
    Map<Integer, Map<Integer, TransmissionFrameQueue>> transmissionFrameQueueTree;
    
    /**
     * Threads used for receiving and transmitting of frames. 
     * A pair for each client partecipating in the session.
     */
    Map<Integer, LockstepTransmitter> transmitters;
    Map<Integer, LockstepReceiver> receivers;

    /**
     * Used for synchronization between server and executionFrameQueues
     */
    Object executionQueuesUpdateMonitor = new Object();
    
    public LockstepServer()
    {
        //Inizializzazione campi, avvio thread...
    }

    /**
     * The server simply cycles collecting a complete set of frame inputs and
     * forwarding them to all the clients. Differently from the clients, it doesn't
     * wait any interframe time to process the executionFrameQueues.
     * If a frame lacks any input from any client, the server stops waiting for
     * them eventually forcing the client to stop for synchronization.
     */
    @Override
    public void run()
    {
        while(true)
        {
            Map<Integer, FrameInput> frameInputs = collectFrames();
            distributeFrameInputs(frameInputs);
        }
    }

    private Map<Integer, FrameInput> collectFrames()
    {
        synchronized(executionQueuesUpdateMonitor)
        {
            while(!checkFrameInputs())
                await();
        }

        Map<Integer, FrameInput> frameInputs = new TreeMap<>();
        for(Entry<Integer, ExecutionFrameQueue> entry : this.executionFrameQueues.entrySet())
        {
            frameInputs.put(entry.getKey(), entry.getValue().pop());
        }
        return frameInputs;
    }

    private boolean checkFrameInputs()
    {
        for(Entry<Integer, ExecutionFrameQueue> entry : this.executionFrameQueues.entrySet())
        {
            if(entry.getValue().head() == null)
                return false;
        }
        return true;
    }

    private void distributeFrameInputs(Map<Integer, FrameInput> frameInputs)
    {
        for(Entry<Integer, FrameInput> frameInputEntry : frameInputs.entrySet())
        {
            for(Entry<Integer, Map<Integer, TransmissionFrameQueue>> transmissionFrameQueueMapEntry : this.transmissionFrameQueueTree.entrySet())
            {
                if(transmissionFrameQueueMapEntry.getKey() != frameInputEntry.getKey())
                {
                    Map<Integer, TransmissionFrameQueue> transmissionFrameQueueMap = transmissionFrameQueueMapEntry.getValue();
                    TransmissionFrameQueue transmissionFrameQueue = transmissionFrameQueueMap.get(frameInputEntry.getKey());
                    transmissionFrameQueue.push(frameInputEntry.getValue());
                }
            }
        }
    }
}
