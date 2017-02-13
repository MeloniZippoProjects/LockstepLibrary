/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mosaicsimulation;

import java.awt.Dimension;
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
public class MosaicClient extends LockstepClient<MosaicCommand> {

    private final Rectangle[][] mosaic;
    private final Color clientColor;
    private final Dimension mosaicDimension;
    
    public MosaicClient(InetSocketAddress serverTCPAddress, Rectangle[][] mosaic, Dimension mosaicDimension, Color clientColor) {
        super(serverTCPAddress);
        this.mosaic = mosaic;
        this.clientColor = clientColor;
        this.mosaicDimension = mosaicDimension;
    }

    @Override
    protected MosaicCommand readInput() {
        Random rand = new Random();
        int x = rand.nextInt(mosaicDimension.width);
        int y = rand.nextInt(mosaicDimension.height);
        
        return new MosaicCommand(clientColor, x, y);
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
        Rectangle rect = mosaic[cmd.getX()][cmd.getY()];
        rect.setFill(cmd.getColor());
    }

    @Override
    protected MosaicCommand[] fillCommands() {
        MosaicCommand[] fillers = new MosaicCommand[10];
        
        for(MosaicCommand cmd : fillers)
        {
            cmd = new MosaicCommand();
        }
        
        return fillers;
    }    
}
