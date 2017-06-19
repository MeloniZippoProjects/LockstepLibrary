/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lockstep.messages.handshake;

import java.io.Serializable;

/**
 * Second message of the handshake protocol, from server to client.
 * It contains the udp port to use during simulation, the assigned clientID,
 * the number of clients that will partecipate and the initial frame number
 */
public class ServerHelloReply implements Serializable
{
    public int serverUDPPort;
    public int assignedClientID;
    public int clientsNumber;
    public int firstFrameNumber;

    public ServerHelloReply(int serverUDPPort, int assignedClientID, int clientsNumber, int firstFrameNumber)
    {
        this.serverUDPPort = serverUDPPort;
        this.assignedClientID = assignedClientID;
        this.clientsNumber = clientsNumber;
        this.firstFrameNumber = firstFrameNumber;
    }
}
