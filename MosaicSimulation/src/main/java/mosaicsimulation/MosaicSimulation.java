/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mosaicsimulation;

import java.util.Map;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
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
    
    @Override
    public void start(Stage stage) throws Exception
    {
        Parent root = FXMLLoader.load(getClass().getResource("FXMLMainPage.fxml"));
        Scene scene = new Scene(root);
        
        mosaic = (GridPane) scene.lookup("#mosaic");
        for (int i = 1; i <= rows; i++)
        {
            for (int j = 1; j <= columns; j++)
            {
                Rectangle rectangle = new Rectangle();
            }
        }
       
        
        
        Map<String, String> namedParameters = this.getParameters().getNamed();
        String serverIPAddress = namedParameters.get("serverIPAddress");
        int serverTCPPort = Integer.parseInt(namedParameters.get("serverTCPPort"));
        
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
