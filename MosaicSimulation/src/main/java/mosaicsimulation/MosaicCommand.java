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
    int row;
    int column;
    
    boolean nop;
    
    /**
     * Creates a command with specified parameters
     * @param color color of the rectangle
     * @param row row coordinate of rectangle
     * @param column column coordinate of rectangle
     */
    public MosaicCommand(Color color, int row, int column)
    {
        this.color = color;
        this.row = row;
        this.column = column;
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

    public int getRow() {
        return row;
    }

    public void setRow(int row) {
        this.row = row;
    }

    public int getColumn() {
        return column;
    }

    public void setColumn(int column) {
        this.column = column;
    }
    
    
    
}
