/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lockstep;

import com.sun.org.apache.xml.internal.utils.SerializableLocatorImpl;
import java.io.Serializable;
import lockstep.messages.FrameACK;
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
    
    final static int bufferSize = 10;
    final static int initialFrame = 4;
    ExecutionFrameQueue efq;
    FrameInput<Command>[] frames;
    
    @Before
    public void setUp() {
        FrameInput frame1 = createFrame(4,1,-1), frame2 = createFrame(5,-1,-1), frame3 = createFrame(6,-1,1); 
        frames = new FrameInput[] { frame1, frame2, frame3 };
    }
    
    @Test
    public void singlePushTest()
    {
        efq = new ExecutionFrameQueue(bufferSize, initialFrame);
        FrameInput<Command> frame = createFrame(4,1,-1);
        efq.push(frame);
        assertSame("Check single push", frame, efq.head());
    }
    
    @Test
    public void multiplePushTest()
    {    
        efq = new ExecutionFrameQueue(bufferSize, initialFrame);
        efq.push(frames);
        
        for(int i = 0; i < 3; i++)
        {
            assertSame("Check multiple push", frames[i], efq.pop());
        }        
    }
    
    @Test
    public void inOrderPop()
    {
        efq = new ExecutionFrameQueue(bufferSize, initialFrame);
        efq.push(createFrame(5,1,1));
        assertEquals("Check in order pop", null, efq.pop());
        
        FrameInput frame = createFrame(4,1,1);
        efq.push(frame);
        assertSame("Check in order pop", frame, efq.pop());
    }
    
    @Test
    public void inOrderAck()
    {
        efq = new ExecutionFrameQueue(bufferSize, initialFrame);
        FrameACK ack = efq.push(frames);
        
        assertEquals("Check cumulative acks", ack.cumulativeACK, 6);
    }
    
    @Test
    public void selectiveAck()
    {
        efq = new ExecutionFrameQueue(bufferSize, initialFrame);
        FrameInput[] frames2 = frames.clone();
        frames2[2] = createFrame(8,1,1);
        
        FrameACK ack = efq.push(frames2);
        
        assertEquals("Check cumulative ACK", 5, ack.cumulativeACK);
        assertArrayEquals("Check selective ACKs", new int[]{8}, ack.selectiveACKs);        
    }
    
    @Test
    public void acksWithGapFilling()
    {
        efq = new ExecutionFrameQueue(bufferSize, initialFrame);
        FrameInput frame1 = createFrame(4,1,1), frame2 = createFrame(6,1,1), frame3 = createFrame(7,1,1);
        FrameInput[] frames2 = new FrameInput[]{ frame1, frame2, frame3 };
        
        FrameACK ack = efq.push(frames2);
        
        assertEquals("Check cumulative ACK", 4, ack.cumulativeACK);
        assertArrayEquals("Check selective ACKs", new int[]{6,7}, ack.selectiveACKs);
        
        ack = efq.push(createFrame(5,1,1));
        assertEquals("Check cumulative ACK", 7, ack.cumulativeACK);
        assertArrayEquals("Check selective ACKs", null, ack.selectiveACKs);    
    }
    
    private FrameInput<Command> createFrame(int n, int upd,int rl)
    {
        return new FrameInput<>(n, new Command(upd, rl));
    }
    
}
