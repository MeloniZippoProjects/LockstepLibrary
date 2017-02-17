/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package xeviousvs.server;

import java.util.Random;
import lockstep.LockstepServer;
import org.apache.log4j.Logger;
import xeviousvs.Comando;

/**
 *
 * @author Raff
 */
public class XeviousVSLockstepServer extends LockstepServer<Comando>
{
    private int serverID;
    private String serverAddress;
    private int tcpPort;
    
    private static final Logger LOG = Logger.getLogger(XeviousVSLockstepServer.class.getName());    
    
    /**
     * Standard constructor, hidden.
     * 
     * @param tcpPort
     * @param clientsNumber
     * @param tickrate 
     */
    private XeviousVSLockstepServer(int tcpPort, int clientsNumber, int tickrate)
    {
        super(tcpPort, clientsNumber, tickrate);
    }    
    
    public XeviousVSLockstepServer(String serverAddress, int tcpPort, int tickrate)
    {
        super(tcpPort, 2, tickrate);
        this.serverAddress = serverAddress;
        this.tcpPort = tcpPort;
        Random rnd = new Random();
        serverID = rnd.nextInt(10000);
        LOG.debug("Server thread created with ID " + serverID);
    }
    
    @Override
    protected void atServerStarted()
    {
        OperazioniDatabaseServer.registraServerDisponibile(serverID, serverAddress, tcpPort);
        LOG.debug("Server registered at database. Handshake phase starting");
    }
    
    @Override
    protected void atHandshakeEnded()
    {
        OperazioniDatabaseServer.rimuoviServerDisponibile(serverID);
        LOG.debug("Server registration removed from database. Game phase starting");
    }
}
