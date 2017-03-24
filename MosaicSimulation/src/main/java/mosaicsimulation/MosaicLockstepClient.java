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
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

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
    
    private static final Logger LOG = LogManager.getLogger(MosaicLockstepClient.class);
    private static final Logger logSim = LogManager.getLogger("Simulation");
    private final Label currentFrameLabel;
    private final int fillSize;
    private final Label currentFPSLabel;
    long throughputTimer = System.currentTimeMillis(); 
    int throughputFramesProcessed = 0;
    double throughput;
    double throughputMeasureInterval = 20;
    double alpha = 0.125;
    
    public MosaicLockstepClient(InetSocketAddress serverTCPAddress, int framerate, int tickrate, int fillTimeout, int fillSize, Rectangle[][] mosaic, int rows, int columns, Color clientColor, Label currentFrameLabel, Label currentFPSLabel) {
        super(serverTCPAddress, framerate, tickrate, fillTimeout);
        this.fillSize = fillSize;
        
        this.mosaic = mosaic;
        this.clientColor = clientColor;
        this.rows = rows;
        this.columns = columns;
        this.currentFrameLabel = currentFrameLabel;
        this.currentFPSLabel = currentFPSLabel;
        this.rand = new Random();
        
        
        
        logSim.log(Level.getLevel("SIMULATION"), "Prova simulazione");
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
            if(clientColor.equals(cmd.getColor()))
            {
                throughputFramesProcessed++;
            }
            if( throughputFramesProcessed == throughputMeasureInterval )
            {
                double measure = throughputMeasureInterval*1000/(System.currentTimeMillis() - throughputTimer);
                throughput = alpha*throughput + (1 - alpha)*measure;
                
                LOG.debug("Throughput = " + throughput);
                Platform.runLater(()-> currentFPSLabel.setText( Double.toString( throughput ).substring( 0, Integer.min( 4, Double.toString( throughput ).length() ) ) ) );
                throughputFramesProcessed = 0;
                throughputTimer = System.currentTimeMillis();
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
