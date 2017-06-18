/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lockstep.messages.handshake;

import java.io.Serializable;

/**
 * Fourth and final message of the handshake protocol, from server to client.
 * It signals the start of the simulation, clients who receive it can start
 * sending frames
 */
public class SimulationStart implements Serializable
{ 
}
