/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lockstep.messages;

/**
 *
 * @author Raff
 */
public abstract class FrameACK implements java.io.Serializable
{
    public final int cumulativeACK;
    public final int[] selectiveACKs;
    
    public FrameACK(int cumulativeACK, int[] selectiveACKs)
    {
        this.cumulativeACK = cumulativeACK;
        this.selectiveACKs = selectiveACKs;
    }
}
