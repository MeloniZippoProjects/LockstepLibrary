/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lockstep;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.Semaphore;
import lockstep.messages.simulation.FrameACK;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author enric
 */



public class TransmissionFrameQueueTest {
    
    public TransmissionFrameQueueTest() {
    }
    
    TransmissionQueue<Command> tfq;
    FrameInput<Command>[] frames;
    
    
    @Before
    public void setUp() {
        Semaphore sem = new Semaphore(0);
        tfq = new TransmissionQueue(7, sem, 5);
        FrameInput frame1 = createFrame(7,1,-1), frame2 = createFrame(8,-1,-1), frame3 = createFrame(9,-1,1); 
        frames = new FrameInput[] { frame1, frame2, frame3 };
    }
    
    @After
    public void tearDown() {
    }

    
    @Test
    public void singlePush()
    {     
        tfq.push(frames[0]);
        FrameInput<Command>[] popped = tfq.pop(); 
        assertArrayEquals("Check single push", new FrameInput[] { frames[0] }, popped);
    }
    
    @Test
    public void multiplePush()
    {
        Command[] cmds = new Command[3];
        for(int i = 0; i < 3; ++i)
            cmds[i] = frames[i].getCommand();
        tfq.push(frames);
        FrameInput<Command>[] popped = tfq.pop();
        assertArrayEquals("Check multiple push", frames, popped);
    }
    
    @Test
    public void cumulativeACK()
    {
        Command[] cmds = new Command[3];
        for(int i = 0; i < 3; ++i)
            cmds[i] = frames[i].getCommand();
        tfq.push(frames);
        FrameACK ack = new FrameACK(8, null);
        tfq.processACK(ack);
        
        FrameInput<Command>[] popped = tfq.pop();
        assertArrayEquals("Check multiple push", new FrameInput[] { frames[2] }, popped);
    }
    
    @Test 
    public void selectiveACK()
    {
        Command[] cmds = new Command[3];
        for(int i = 0; i < 3; ++i)
            cmds[i] = frames[i].getCommand();
        tfq.push(frames);
        FrameACK ack = new FrameACK(7, new int[] { 9 } );
        tfq.processACK(ack);
        
        FrameInput<Command>[] popped = tfq.pop();
        assertArrayEquals("Check multiple push", new FrameInput[] { frames[1] }, popped);
    }
    
    @Test
    public void emptyTFQ()
    {
        FrameInput<Command>[] inputs = tfq.pop();
        
    }
    
    private FrameInput<Command> createFrame(int n, int upd,int rl)
    {
        return new FrameInput<>(n, new Command(upd, rl));
    }
}
