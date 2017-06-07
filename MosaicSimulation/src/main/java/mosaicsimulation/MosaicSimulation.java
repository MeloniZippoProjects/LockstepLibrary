/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mosaicsimulation;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Random;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import lockstep.LockstepClient;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 *
 * @author Raff
 */
public class MosaicSimulation extends Application
{
    static final int columns = 50;
    static final int rows = 50;
    
    GridPane mosaicView;
    Rectangle[][] mosaic;
    Color clientColor;
    
    private static final Logger LOG = LogManager.getLogger(MosaicSimulation.class.getName());
    
    @Override
    public void start(Stage stage) throws Exception
    {
        stage.setTitle("Mosaic simulation");
        Parent root = FXMLLoader.load(getClass().getClassLoader().getResource("FXMLMainPage.fxml"));
        Scene scene = new Scene(root);
        
        mosaicView = (GridPane) scene.lookup("#mosaic");
        mosaic = new Rectangle[rows][];
        for (int row = 1; row <= rows; row++)
        {
            mosaic[row - 1] = new Rectangle[columns];
            for (int column = 1; column <= columns; column++)
            {
                Rectangle rectangle = new Rectangle();
                rectangle.setHeight(5);
                rectangle.setWidth(5);
                rectangle.setStrokeWidth(1);
                rectangle.setStroke(Color.GRAY);
                rectangle.setFill(Color.BLACK);
                
                GridPane.setConstraints(rectangle, column, row);
                mosaicView.getChildren().add(rectangle);
                mosaic[row - 1][column - 1] = rectangle;
            }
        }      
        
        Random rand = new Random();
        clientColor = Color.rgb(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256));
        Rectangle colorRectangle = (Rectangle) scene.lookup("#colorRectangle");
        colorRectangle.setFill(clientColor);
        
        stage.setScene(scene);
        stage.show();
        
        LOG.info("created graphical UI");
        
        Map<String, String> namedParameters = this.getParameters().getNamed();
        String serverIPAddress = namedParameters.get("serverIPAddress");
        int serverTCPPort = Integer.parseInt(namedParameters.get("serverTCPPort"));
        int framerate = Integer.parseInt(namedParameters.get("framerate"));
        int tickrate = Integer.parseInt(namedParameters.get("tickrate"));
        int fillTimeout = Integer.parseInt(namedParameters.get("fillTimeout"));
        int fillSize = Integer.parseInt(namedParameters.get("fillSize"));
        InetSocketAddress serverTCPAddress = new InetSocketAddress(serverIPAddress, serverTCPPort);

        Label currentFrameLabel = (Label) scene.lookup("#currentFrameLabel");
        
        Label currentFPSLabel = (Label) scene.lookup("#frameRate");
        
        LOG.debug("Creation of lockstep client");
        MosaicLockstepClient mosaicLockstepClient = new MosaicLockstepClient(serverTCPAddress, framerate, tickrate, fillTimeout, fillSize, mosaic, rows, columns, clientColor, currentFrameLabel, currentFPSLabel);
        
        LockstepClient lockstepClient = new LockstepClient(serverTCPAddress, framerate, tickrate, fillTimeout, mosaicLockstepClient);
        
        Thread clientThread = new Thread(lockstepClient);
        clientThread.setName("main-client-thread");
        clientThread.start();
        LOG.debug("thread started");
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        launch(args);
    }
    
}
