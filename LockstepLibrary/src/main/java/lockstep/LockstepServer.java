/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lockstep;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    Map<Integer, Boolean> executionQueuesHeadsAvailability;
    
    public LockstepServer()
    {
        //Inizializzazione campi, avvio thread...
    }

    /**
     * The server simply cycles collecting a complete set of frame inputs and
     * forwarding them to all the clients. Differently from the clients, it doesn't
     * wait any interframe time to process the executionFrameQueues.
     * If a frame lacks any input from any client, the server stops and waits for
     * them eventually forcing the clients to stop for synchronization.
     */
    @Override
    public void run()
    {
        while(true)
        {
            try
            {
                if(executionQueuesHeadsAvailability.containsValue(Boolean.FALSE))
                {
                    for(Integer key : executionQueuesHeadsAvailability.keySet())
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

                Map<Integer, FrameInput> frameInputs = collectFrameInputs();
                distributeFrameInputs(frameInputs);
            } catch (InterruptedException ex)
            {
                Logger.getLogger(LockstepServer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    private Map<Integer, FrameInput> collectFrameInputs()
    {
        Map<Integer, FrameInput> frameInputs = new TreeMap<>();
        for(Entry<Integer, ExecutionFrameQueue> entry : this.executionFrameQueues.entrySet())
        {
            frameInputs.put(entry.getKey(), entry.getValue().pop());
        }
        return frameInputs;
    }
    
    private void distributeFrameInputs(Map<Integer, FrameInput> frameInputs)
    {
        //For each frameInput
        for(Entry<Integer, FrameInput> frameInputEntry : frameInputs.entrySet())
        {
            //For each client, take its tree of transmission queues
            for(Entry<Integer, Map<Integer, TransmissionFrameQueue>> transmissionFrameQueueMapEntry : this.transmissionFrameQueueTree.entrySet())
            {
                //If the frameInput doesn't come from that client, forward the frameInput though the correct transmission queue
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
