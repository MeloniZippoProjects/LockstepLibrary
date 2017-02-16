package xeviousvs.server;



import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;


class Comando implements Externalizable
{

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
    
    enum EnumComando {

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
    
    String username;
    EnumComando comando;
    
    public Comando(EnumComando comando, String username)
    {
        this.username = username;
        this.comando = comando;
    }
}

