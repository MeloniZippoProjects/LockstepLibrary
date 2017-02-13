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
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import org.apache.log4j.Logger;

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
    
    private static final Logger LOG = Logger.getLogger(MosaicSimulation.class.getName());
    
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
                rectangle.setHeight(7);
                rectangle.setWidth(7);
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
        InetSocketAddress serverTCPAddress = new InetSocketAddress(serverIPAddress, serverTCPPort);

        LOG.debug("Creation of lockstep client");
        MosaicLockstepClient mosaicLockstepClient = new MosaicLockstepClient(serverTCPAddress, mosaic, rows, columns, clientColor);
        Thread clientThread = new Thread(mosaicLockstepClient);
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
