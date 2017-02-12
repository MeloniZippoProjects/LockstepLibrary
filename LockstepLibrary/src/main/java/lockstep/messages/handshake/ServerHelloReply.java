/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lockstep.messages.handshake;

import java.io.Serializable;

/**
 *
 * @author Raff
 */
public class ServerHelloReply implements Serializable
{
    public int serverUDPPort;
    public int assignedHostID;
    public int clientsNumber;
    public int firstFrameNumber;
}
