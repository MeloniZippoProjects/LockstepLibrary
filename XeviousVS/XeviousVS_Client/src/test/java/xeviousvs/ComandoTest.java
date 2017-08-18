/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package xeviousvs;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import xeviousvs.Comando.EnumComando;

/**
 *
 * @author Raff
 */
public class ComandoTest
{
    
    public ComandoTest()
    {
    }
    
    @Before
    public void setUp()
    {
    }
    
    @After
    public void tearDown()
    {
    }

   
    @Test
    public void testNewComando()
    {
        assertEquals(EnumComando.Presentazione, EnumComando.newComando(7));
    }
    
    @Test
    public void testExternal() throws IOException, ClassNotFoundException
    {
        Comando c = new Comando(EnumComando.Presentazione, "ciao");
        FileOutputStream fos = new FileOutputStream("testfile");
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(c);
        oos.flush();
        oos.close();

        Comando d;
        FileInputStream fis = new FileInputStream("testfile");
        ObjectInputStream ois = new ObjectInputStream(fis);
        d = (Comando)ois.readObject();
        ois.close();

        assertEquals(c.comando, d.comando);
        assertEquals(c.username, d.username);
    }
    
}
