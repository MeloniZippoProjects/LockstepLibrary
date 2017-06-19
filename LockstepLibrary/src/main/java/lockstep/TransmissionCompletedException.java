/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lockstep;

import java.io.IOException;

/**
 * Used inside a Transmitter to catch the event where, after an interruption
 * has been received, the transmissionQueue becomes empty
 */
public class TransmissionCompletedException extends IOException
{
    
}
