/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mosaicsimulation;

import java.net.InetSocketAddress;
import java.util.Random;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import lockstep.LockstepClient;
import org.apache.log4j.Logger;

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
    
    private static final Logger LOG = Logger.getLogger(MosaicLockstepClient.class.getName());
    private final Label currentFrameLabel;
    private final int fillSize;
    
    public MosaicLockstepClient(InetSocketAddress serverTCPAddress, int framerate, int tickrate, int fillTimeout, int fillSize, Rectangle[][] mosaic, int rows, int columns, Color clientColor, Label currentFrameLabel) {
        super(serverTCPAddress, framerate, tickrate, fillTimeout);
        this.fillSize = fillSize;
        
        this.mosaic = mosaic;
        this.clientColor = clientColor;
        this.rows = rows;
        this.columns = columns;
        this.currentFrameLabel = currentFrameLabel;
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
        LOG.debug("Simulation suspended");
    }

    @Override
    protected void resumeSimulation() {
        LOG.debug("Simulation resumed");
    }

    @Override
    protected void executeCommand(MosaicCommand cmd)
    {
        try{
            if(!cmd.nop)
            {
                Rectangle rect = mosaic[cmd.getRow()][cmd.getColumn()];
                Platform.runLater(() -> rect.setFill(cmd.getColor()));
            }
            Platform.runLater(()-> currentFrameLabel.setText(Integer.toString(cmd.ownFrame)));
        }  
        catch(NullPointerException e)
        {
            System.exit(1);
        }
            
    }

    @Override
    protected MosaicCommand[] fillCommands() {
        MosaicCommand[] fillers = new MosaicCommand[fillSize];
        
        for (int i = 0; i < fillers.length; i++)
        {
            fillers[i] = new MosaicCommand();
        }
        
        return fillers;
    }   
    
    @Override
    protected MosaicCommand[] bootstrapCommands() {
        MosaicCommand[] fillers = new MosaicCommand[fillSize*2];
        
        for (int i = 0; i < fillers.length; i++)
        {
            fillers[i] = new MosaicCommand();
        }
        
        return fillers;
    }    
}
