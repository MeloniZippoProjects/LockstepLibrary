/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lockstep;

import java.io.Serializable;

/**
 *
 * @author enric
 */
class Command implements Serializable
{
    int up_down;
    int right_left;
    
    public Command(int upd, int rl)
    {
        up_down = upd;
        right_left = rl;
    }
}