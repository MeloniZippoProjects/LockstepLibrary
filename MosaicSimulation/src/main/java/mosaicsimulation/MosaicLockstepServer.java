/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mosaicsimulation;

import lockstep.LockstepServer;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;

/**
 *
 * @author enric
 */
public class MosaicLockstepServer {
    private static final Logger LOG = org.apache.log4j.Logger.getLogger(MosaicLockstepServer.class.getName());
    
    public static void main(String[] args)
    {
        Thread thread = new Thread(new LockstepServer<MosaicCommand>(Integer.parseInt(args[0]), Integer.parseInt(args[1])));
        thread.setName("Main-server-thread");
        thread.start();
        
        try {     
            thread.join();
        } catch (InterruptedException ex) {
            LOG.error("Server interrupted while joining");
        }
    }
    
}
