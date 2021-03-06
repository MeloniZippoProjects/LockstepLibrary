/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lockstep;

import com.sun.org.apache.xml.internal.utils.SerializableLocatorImpl;
import java.io.Serializable;
import java.util.concurrent.Semaphore;
import lockstep.messages.simulation.FrameACK;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import lockstep.Command;
import static org.junit.Assert.*;


/**
 *
 * @author enric
 */

public class ExecutionFrameQueueTest {
    
   
    public ExecutionFrameQueueTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    final static int initialFrame = 4;
    ClientReceivingQueue efq;
    FrameInput[] frames;
    
    @Before
    public void setUp() {
        Semaphore sem = new Semaphore(0);
        efq = new ClientReceivingQueue(initialFrame, 1, sem);
        FrameInput frame1 = createFrame(4,1,-1), frame2 = createFrame(5,-1,-1), frame3 = createFrame(6,-1,1); 
        frames = new FrameInput[] { frame1, frame2, frame3 };
    }
    
    @Test
    public void singlePushTest()
    {
        FrameInput frame = createFrame(4,1,-1);
        efq.push(frame);
        assertSame("Check single push", frame.getFrameNumber(), efq.head().getFrameNumber());
        assertSame("Check single push", frame.getCommand(), efq.head().getCommand());
    }
    
    @Test
    public void multiplePushTest()
    {
        efq.push(frames);
        
        for(int i = 0; i < 3; i++)
        {
            assertSame("Check multiple push", frames[i].getCommand(), efq.pop().getCommand());
        }        
    }
    
    @Test
    public void inOrderPop()
    {
        efq.push(createFrame(5,1,1));
        assertEquals("Check in order pop", null, efq.pop());
        
        FrameInput frame = createFrame(4,1,1);
        efq.push(frame);
        assertSame("Check in order pop", frame.getCommand(), efq.pop().getCommand());
    }
    
    @Test
    public void inOrderAck()
    {
        FrameACK ack = efq.push(frames);
        
        assertEquals("Check cumulative acks", ack.cumulativeACK, 6);
    }
    
    @Test
    public void selectiveAck()
    {
        FrameInput[] frames2 = frames.clone();
        frames2[2] = createFrame(8,1,1);
        
        FrameACK ack = efq.push(frames2);
        
        assertEquals("Check cumulative ACK", 5, ack.cumulativeACK);
        assertArrayEquals("Check selective ACKs", new int[]{8}, ack.selectiveACKs);        
    }
    
    @Test
    public void acksWithGapFilling()
    {
        FrameInput frame1 = createFrame(4,1,1), frame2 = createFrame(6,1,1), frame3 = createFrame(7,1,1);
        FrameInput[] frames2 = new FrameInput[]{ frame1, frame2, frame3 };
        
        FrameACK ack = efq.push(frames2);
        
        assertEquals("Check cumulative ACK", 4, ack.cumulativeACK);
        assertArrayEquals("Check selective ACKs", new int[]{6,7}, ack.selectiveACKs);
        
        ack = efq.push(createFrame(5,1,1));
        assertEquals("Check cumulative ACK", 7, ack.cumulativeACK);
        assertArrayEquals("Check selective ACKs", null, ack.selectiveACKs);    
    }
    
    private FrameInput createFrame(int n, int upd,int rl)
    {
        return new FrameInput(n, new Command(upd, rl));
    }
    
}
