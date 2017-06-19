/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mosaicsimulation;

import java.util.Random;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import lockstep.LockstepApplication;
import lockstep.LockstepClient;
import lockstep.messages.simulation.LockstepCommand;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class MosaicLockstepApplication implements LockstepApplication {
    private final Rectangle[][] mosaic;
    private final Color clientColor;
    private final int rows;
    private final int columns;
    private final Random rand;
    
    private final int fillSize;
    
    private static final Logger LOG = LogManager.getLogger(MosaicLockstepApplication.class);
    private final Label currentFrameLabel;
    private final Label currentFPSLabel;
    long throughputTimer = System.currentTimeMillis(); 
    int throughputFramesProcessed = 0;
    double throughput;
    double throughputMeasureInterval = 20;
    double alpha = 0.125;
    private LockstepClient lockstepClient;
    private final boolean abortOnDisconnect;
    private final int frameLimit;
    
    public MosaicLockstepApplication(Rectangle[][] mosaic, int rows, int columns,
            Color clientColor, Label currentFrameLabel,
            Label currentFPSLabel, boolean abortOnDisconnect, int fillsize, int frameLimit)
    {
        //super(serverTCPAddress, framerate, tickrate, fillTimeout);
         this.mosaic = mosaic;
        this.clientColor = clientColor;
        this.rows = rows;
        this.columns = columns;
        this.currentFrameLabel = currentFrameLabel;
        this.currentFPSLabel = currentFPSLabel;
        this.rand = new Random();
        this.abortOnDisconnect = abortOnDisconnect;
        this.fillSize = fillsize;
        this.frameLimit = frameLimit;
    }

    public static class Builder {

        private Rectangle[][] mosaic;
        private Color clientColor;
        private int rows;
        private int columns;
        private Random rand;
        private int fillSize;
        private Label currentFrameLabel;
        private Label currentFPSLabel;
        private long throughputTimer;
        private int throughputFramesProcessed;
        private double throughput;
        private double throughputMeasureInterval;
        private double alpha;
        private LockstepClient clientThread;
        private boolean abortOnDisconnect;
        private int frameLimit;

        private Builder() {
        }

        public Builder mosaic(final Rectangle[][] value) {
            this.mosaic = value;
            return this;
        }

        public Builder clientColor(final Color value) {
            this.clientColor = value;
            return this;
        }

        public Builder rows(final int value) {
            this.rows = value;
            return this;
        }

        public Builder columns(final int value) {
            this.columns = value;
            return this;
        }

        public Builder rand(final Random value) {
            this.rand = value;
            return this;
        }

        public Builder fillSize(final int value) {
            this.fillSize = value;
            return this;
        }

        public Builder currentFrameLabel(final Label value) {
            this.currentFrameLabel = value;
            return this;
        }

        public Builder currentFPSLabel(final Label value) {
            this.currentFPSLabel = value;
            return this;
        }

        public Builder clientThread(final LockstepClient value) {
            this.clientThread = value;
            return this;
        }

        public Builder abortOnDisconnect(final boolean value) {
            this.abortOnDisconnect = value;
            return this;
        }
        
        public Builder frameLimit(final int value)
        {
            this.frameLimit = value;
            return this;
        }

        public MosaicLockstepApplication build() {
            //return new mosaicsimulation.MosaicLockstepApplication(mosaic, clientColor, rows, columns, rand, fillSize, currentFrameLabel, currentFPSLabel, throughputTimer, throughputFramesProcessed, throughput, throughputMeasureInterval, alpha, lockstepClient, abortOnDisconnect);
            return new mosaicsimulation.MosaicLockstepApplication(mosaic, rows, 
                    columns, clientColor, 
                    currentFrameLabel, currentFPSLabel, 
                    abortOnDisconnect, fillSize, frameLimit);
        }
    }

    public static MosaicLockstepApplication.Builder builder() {
        return new MosaicLockstepApplication.Builder();
    }
    
    
    @Override
    public MosaicCommand readInput() {
        int row = rand.nextInt(rows);
        int column = rand.nextInt(columns);
        
        return new MosaicCommand(clientColor, row, column);
    }

    @Override
    public void suspendSimulation() {
        LOG.debug("Simulation suspended");
    }

    @Override
    public void resumeSimulation() {
        LOG.debug("Simulation resumed");
    }

    @Override
    public void executeCommand(LockstepCommand lstepCmd)
    {
        MosaicCommand cmd = (MosaicCommand)lstepCmd;
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
            
            if(frameLimit > 0 && cmd.ownFrame == frameLimit)
                lockstepClient.abort();
        }  
        catch(NullPointerException e)
        {
            System.exit(1);
        }
            
    }

    @Override
    public MosaicCommand[] fillCommands() {
        MosaicCommand[] fillers = new MosaicCommand[fillSize];
        
        for (int i = 0; i < fillers.length; i++)
        {
            fillers[i] = new MosaicCommand();
        }
        
        return fillers;
    }   
    
    @Override
    public MosaicCommand[] bootstrapCommands() {
        MosaicCommand[] fillers = new MosaicCommand[fillSize*2];
        
        for (int i = 0; i < fillers.length; i++)
        {
            fillers[i] = new MosaicCommand();
        }
        
        return fillers;
    }    

    @Override
    public void signalHandshakeFailure()
    {
        System.exit(1);
    }

    @Override
    public void signalDisconnection(int remainingClients)
    {
        if(abortOnDisconnect)
            lockstepClient.abort();
    }

    void setLockstepClient(LockstepClient lockstepClient) {
        this.lockstepClient = lockstepClient;
    }
}
