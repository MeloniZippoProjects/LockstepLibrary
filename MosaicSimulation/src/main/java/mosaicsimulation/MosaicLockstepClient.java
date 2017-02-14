/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mosaicsimulation;

import java.net.InetSocketAddress;
import java.util.Random;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import lockstep.FrameInput;
import lockstep.LockstepClient;

/**
 *
 * @author enric
 */
public class MosaicLockstepClient extends LockstepClient<MosaicCommand> {

    private final Rectangle[][] mosaic;
    private final Color clientColor;
    private final int rows;
    private final int columns;
    private final Random rand;
    
    public MosaicLockstepClient(InetSocketAddress serverTCPAddress, Rectangle[][] mosaic, int rows, int columns, Color clientColor) {
        super(serverTCPAddress);
        this.mosaic = mosaic;
        this.clientColor = clientColor;
        this.rows = rows;
        this.columns = columns;
        this.rand = new Random();
    }

    @Override
    protected MosaicCommand readInput() {
        int row = rand.nextInt(rows);
        int column = rand.nextInt(columns);
        
        return new MosaicCommand(clientColor, row, column);
    }

    @Override
    protected void suspendSimulation() {
        return;
    }

    @Override
    protected void resumeSimulation() {
        return;
    }

    @Override
    protected void executeFrameInput(FrameInput<MosaicCommand> f) {
        MosaicCommand cmd = f.getCmd();
        if(!cmd.nop)
        {
            Rectangle rect = mosaic[cmd.getRow()][cmd.getColumn()];
            rect.setFill(cmd.getColor());
        }   
    }

    @Override
    protected MosaicCommand[] fillCommands() {
        MosaicCommand[] fillers = new MosaicCommand[10];
        
        for (int i = 0; i < 10; i++)
        {
            fillers[i] = new MosaicCommand();
        }
        
        return fillers;
    }    
}
