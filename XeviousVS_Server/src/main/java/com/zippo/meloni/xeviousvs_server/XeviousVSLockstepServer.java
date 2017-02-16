/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.zippo.meloni.xeviousvs_server;

import java.util.Random;
import lockstep.LockstepServer;

/**
 *
 * @author Raff
 */
public class XeviousVSLockstepServer extends LockstepServer<Comando>
{
    private int serverID;
    private String serverAddress;
    private int tcpPort;
    
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
        Random rnd = new Random();
        serverID = rnd.nextInt(10000);
    }
    
    @Override
    protected void atServerStarted()
    {
        OperazioniDatabaseServer.registraServerDisponibile(serverID, serverAddress, tcpPort);
    }
    
    @Override
    protected void atHandshakeEnded()
    {
        OperazioniDatabaseServer.rimuoviServerDisponibile(serverID);
    }
}
