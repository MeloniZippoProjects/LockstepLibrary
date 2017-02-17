package xeviousvs;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;


public class Comando implements Externalizable
{
    public enum EnumComando {

        NOP,
        Destra,
        Sinistra,
        Fuoco,
        FuocoDestra,
        FuocoSinistra,
        Pausa,
        Presentazione;
                
        static EnumComando newComando(int value)
        {
            return EnumComando.values()[value];
        }
    }
    
    public String username;
    public EnumComando comando;
    
    public Comando()
    {
        this.comando = EnumComando.NOP;
        this.username = null;
    }
    
    public Comando(Comando c)
    {
        this.comando = c.comando;
        this.username = c.username;
    }
    
    public Comando(EnumComando comando, String username)
    {
        this.username = username;
        this.comando = comando;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
        out.writeUTF(username);
        out.writeInt(comando.ordinal());
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        username = in.readUTF();
        comando = EnumComando.newComando(in.readInt());
    }

    public void reset()
    {
        this.comando = EnumComando.NOP;
    }
}

