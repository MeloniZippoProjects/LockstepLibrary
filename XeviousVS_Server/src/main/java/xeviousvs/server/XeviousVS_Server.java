/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package xeviousvs.server;

import org.apache.log4j.Logger;
import org.apache.commons.cli.*;

/**
 *
 * @author Raff
 */
public class XeviousVS_Server
{
    private static final Logger LOG = Logger.getLogger(XeviousVS_Server.class.getName());   
    
    public static void main(String args[])
    {
        Options opts = new Options();
        opts.addOption("i", "serverAddress", true, "IP address of the server");
        opts.addOption("p", "serverPort", true, "Listening TCP port used to initiate handshakes");
        opts.addOption("d", "databaseAddress", true, "IP address of the matchmaking database");
        opts.addOption("f", "databasePort", true, "Listening port of the matchmaking database");
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
        
        String databaseAddress = commandLine.getOptionValue("databaseAddress");
        int databasePort = Integer.parseInt(commandLine.getOptionValue("databasePort"));
        OperazioniDatabaseServer.impostaIndirizzoDatabase(databaseAddress, databasePort);
        
        String serverAddress = commandLine.getOptionValue("serverAddress");
        int serverPort = Integer.parseInt(commandLine.getOptionValue("serverPort"));
        int tickrate = Integer.parseInt(commandLine.getOptionValue("tickrate"));
        int connectionTimeout = Integer.parseInt(commandLine.getOptionValue("connectionTimeout"));
        
        LOG.debug("Server started, command line parsed");
        
        XeviousVSLockstepServer lockstepServer =new XeviousVSLockstepServer(serverAddress, serverPort, tickrate, connectionTimeout);
        lockstepServer.setName("Main-server-thread");
        lockstepServer.start();
        
        try {     
            lockstepServer.join();
        } catch (InterruptedException ex) {
            LOG.error("Server interrupted while joining");
        }
    }
}
