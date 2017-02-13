/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mosaicsimulation;

import java.util.Random;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

/**
 *
 * @author Raff
 */
public class MosaicSimulation extends Application
{
    static final int columns = 50;
    static final int rows = 50;
    
    GridPane mosaic;
    Color clientColor;
    
    @Override
    public void start(Stage stage) throws Exception
    {
        stage.setTitle("Mosaic simulation");
        Parent root = FXMLLoader.load(getClass().getClassLoader().getResource("FXMLMainPage.fxml"));
        Scene scene = new Scene(root);
        
        mosaic = (GridPane) scene.lookup("#mosaic");
        for (int row = 1; row <= rows; row++)
        {
            for (int column = 1; column <= columns; column++)
            {
                Rectangle rectangle = new Rectangle();
                rectangle.setHeight(7);
                rectangle.setWidth(7);
                rectangle.setStrokeWidth(1);
                rectangle.setStroke(Color.GRAY);
                rectangle.setFill(Color.BLACK);
                
                GridPane.setConstraints(rectangle, column, row);
                mosaic.getChildren().add(rectangle);
            }
        }      
        
        Random rand = new Random();
        Color clientColor = Color.rgb(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256));
        Rectangle colorRectangle = (Rectangle) scene.lookup("#colorRectangle");
        colorRectangle.setFill(clientColor);
        
        
//        Map<String, String> namedParameters = this.getParameters().getNamed();
//        String serverIPAddress = namedParameters.get("serverIPAddress");
//        int serverTCPPort = Integer.parseInt(namedParameters.get("serverTCPPort"));

        
                
        stage.setScene(scene);
        stage.show();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        launch(args);
    }
    
}
