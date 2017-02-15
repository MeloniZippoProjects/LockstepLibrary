/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lockstep.messages.simulation;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Data structure to contain an ACKnowledgement response.
 * It consists of a cumulativeACK, indicating the number of the latest in order 
 * frame received; and a selectiveACKs array indicating the frame received out
 * of order. The selectiveACKs array can be null
 * 
 * @author Raff
 */
public class FrameACK implements java.io.Externalizable
{
    public int senderID;
    public int cumulativeACK;
    public int[] selectiveACKs;
    
    public FrameACK()
    {
        this.senderID = 0;
        this.cumulativeACK = 0;
        this.selectiveACKs = null;
    }
    
    public FrameACK(int hostID, int cumulativeACK, int[] selectiveACKs)
    {
        this.senderID = hostID;
        this.cumulativeACK = cumulativeACK;
        this.selectiveACKs = selectiveACKs;
    }
    
    public FrameACK(int cumulativeACK, int[] selectiveACKs)
    {
        this.cumulativeACK = cumulativeACK;
        this.selectiveACKs = selectiveACKs;
        this.senderID = -1;
    }
    
    public int getSenderID()
    {
        return senderID;
    }
    
    public void setSenderID(int senderID)
    {
        this.senderID = senderID;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
        out.writeInt(senderID);
        out.writeInt(cumulativeACK);
        out.writeObject(selectiveACKs);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        senderID = in.readInt();
        cumulativeACK = in.readInt();
        selectiveACKs = (int[]) in.readObject();
    }
}
