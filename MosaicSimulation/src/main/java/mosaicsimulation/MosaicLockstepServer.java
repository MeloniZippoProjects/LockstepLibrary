/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mosaicsimulation;

import java.util.logging.Level;
import lockstep.LockstepServer;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.commons.cli.*;

/**
 *
 * @author enric
 */
public class MosaicLockstepServer {
    private static final Logger LOG = LogManager.getLogger(MosaicLockstepServer.class);
    
    public static void main(String[] args)
    {
        Options opts = new Options();
        opts.addOption("s", "serverPort", true, "Listening TCP port used to initiate handshakes");
        opts.addOption("n", "nClients", true, "Number of clients that will participate in the session");
        opts.addOption("t", "tickrate", true, "Number of transmission session to execute per second");
        opts.addOption("c", "connectionTimeout", true, "Timeout for UDP connections");
        
        DefaultParser parser = new DefaultParser();
        CommandLine commandLine = null;
        try
        {
            commandLine = parser.parse(opts, args);
        } catch (ParseException ex)
        {
            ex.printStackTrace();
            System.exit(1);
        }
        
        int serverPort = Integer.parseInt(commandLine.getOptionValue("serverPort"));
        int nClients = Integer.parseInt(commandLine.getOptionValue("nClients"));
        int tickrate = Integer.parseInt(commandLine.getOptionValue("tickrate"));
        int connectionTimeout = Integer.parseInt(commandLine.getOptionValue("connectionTimeout"));
        
        Thread serverThread = LockstepServer.builder()
                .clientsNumber(nClients)
                .tcpPort(serverPort)
                .tickrate(tickrate)
                .connectionTimeout(connectionTimeout)
                .build();
        
        serverThread.setName("Main-server-thread");
        serverThread.start();
        
        try {     
            serverThread.join();
        } catch (InterruptedException ex) {
            LOG.error("Server interrupted while joining");
        }
    }
    
}
