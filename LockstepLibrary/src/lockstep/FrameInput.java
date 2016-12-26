/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lockstep;

import java.io.Serializable;

/**
 *
 * @author Raff
 * @param <Command>
 */
public class FrameInput<Command extends Serializable> implements Serializable 
{
    int frameNumber;
    Command cmd;
}
