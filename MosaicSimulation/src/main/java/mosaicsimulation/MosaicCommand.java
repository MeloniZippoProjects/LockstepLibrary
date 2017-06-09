/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mosaicsimulation;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import javafx.scene.paint.Color;
import lockstep.messages.simulation.LockstepCommand;

/**
 *
 * @author enric
 */
public class MosaicCommand implements Externalizable, LockstepCommand{
    static int frame = 0;
    double colorRed;
    double colorGreen;
    double colorBlue;
    
    int row;
    int column;
    
    boolean nop;
    int ownFrame;
    
    /**
     * Creates a command with specified parameters
     * @param color color of the rectangle
     * @param row row coordinate of rectangle
     * @param column column coordinate of rectangle
     */
    public MosaicCommand(Color color, int row, int column)
    {
        this.colorRed = color.getRed();
        this.colorBlue = color.getBlue();
        this.colorGreen = color.getGreen();
        
        this.row = row;
        this.column = column;
        this.nop = false;
        this.ownFrame = frame++;
    }
    
    /**
     * Creates a MosaicCommand with no operation
     */
    public MosaicCommand()
    {
        this.nop = !nop;
        this.ownFrame = frame++;
    }

    public Color getColor() {
        return Color.color(colorRed, colorGreen, colorBlue);
    }

    public void setColor(Color color) {
        this.colorRed = color.getRed();
        this.colorBlue = color.getBlue();
        this.colorGreen = color.getGreen();
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

    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
        out.writeDouble(colorRed);
        out.writeDouble(colorGreen);
        out.writeDouble(colorBlue);
        out.writeInt(row);
        out.writeInt(column);
        out.writeBoolean(nop);
        out.writeInt(ownFrame);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        colorRed = in.readDouble();
        colorGreen = in.readDouble();
        colorBlue = in.readDouble();
        row = in.readInt();
        column = in.readInt();
        nop = in.readBoolean();
        ownFrame = in.readInt();
        frame--;
    }
}
