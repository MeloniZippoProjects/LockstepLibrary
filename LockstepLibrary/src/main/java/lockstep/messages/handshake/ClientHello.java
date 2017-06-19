/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lockstep.messages.handshake;

import java.io.Serializable;

/**
 * First message of the handshake protocol, from client to server.
 * It contains the udp port for the client, while the IP address
 * is derived from the connection.
 */
public class ClientHello implements Serializable
{
    public int clientUDPPort;
}
