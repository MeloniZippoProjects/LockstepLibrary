/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lockstep.messages.handshake;

import java.io.Serializable;

/**
 * Third message of the handshake protocol, from server to client.
 * It contains the IDs of all the clients partecipating.
 */
public class ClientsAnnouncement implements Serializable
{
    public int[] clientIDs;
}
