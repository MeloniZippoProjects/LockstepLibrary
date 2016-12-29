/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lockstep;

import java.util.Map;

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
     * Buffer for frame input to sent to clients. 
     * One for each client participating in the session.
     */
    Map<Integer, TransmissionFrameQueue> transmissionFrameQueue;
    
    /**
     * Threads used for receiving and transmitting of frames. 
     * A pair for each client partecipating in the session.
     */
    Map<Integer, Transmitter> transmitters;
    Map<Integer, Receiver> receivers;
    
    public LockstepServer()
    {
        //Inizializzazione campi, avvio thread...
    }

    @Override
    public void run()
    {
        while(true)
        {
            
        }
    }
}
