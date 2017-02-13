/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mosaicsimulation;

import java.io.Serializable;
import javafx.scene.paint.Color;

/**
 *
 * @author enric
 */
public class MosaicCommand implements Serializable{
    Color color;
    int x;
    int y;
    
    boolean nop;
    
    /**
     * Creates a command with specified parameters
     * @param color color of the rectangle
     * @param x x coordinate of rectangle
     * @param y y coordinate of rectangle
     */
    public MosaicCommand(Color color, int x, int y)
    {
        this.color = color;
        this.x = x;
        this.y = y;
        this.nop = false;
    }
    
    /**
     * Creates a MosaicCommand with no operation
     */
    public MosaicCommand()
    {
        this.nop = !nop;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }
    
    
    
}
