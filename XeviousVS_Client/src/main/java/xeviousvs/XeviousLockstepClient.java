/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package xeviousvs;
import java.net.InetSocketAddress;
import lockstep.*;
import org.apache.log4j.Logger;
import xeviousvs.Comando.EnumComando;

/**
 *
 * @author enric
 */
public class XeviousLockstepClient extends LockstepClient<Comando>{

    
    public final Comando comandoCorrente;
    public final String proprioUsername;
    public String avversarioUsername;
    public final ModelloGioco modelloGioco;
    
    private final int fillSize;
    
    private static final Logger LOG = Logger.getLogger(XeviousLockstepClient.class.getName());
    
    @Override
    protected Comando readInput() {
        return comandoCorrente;
    }

    @Override
    protected void suspendSimulation() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void resumeSimulation() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void executeCommand(Comando c) {
        
        if(c.comando == EnumComando.Presentazione)
            avversarioUsername = c.username;
        else 
        {
            if(c.username.equals(proprioUsername))
                modelloGioco.eseguiComandoGiocatore(c);
            else if(c.username.equals(avversarioUsername))
                modelloGioco.eseguiComandoAvversario(c);
            else
                LOG.fatal("comando.username non valido");
        }
    }

    @Override
    protected Comando[] fillCommands() {
        Comando[] fillers = new Comando[fillSize];
        
        for(int i = 0; i < fillers.length; ++i)
            fillers[i] = new Comando(EnumComando.NOP, proprioUsername);
        
        return fillers;
    }

    @Override
    protected Comando[] bootstrapCommands() {
        Comando[] fillers = new Comando[fillSize*2];
        fillers[0] = new Comando(EnumComando.Presentazione, proprioUsername);        
        for(int i = 1; i < fillers.length; ++i)
            fillers[i] = new Comando(EnumComando.NOP, proprioUsername);
        
        return fillers;
    }
    
    public XeviousLockstepClient(InetSocketAddress serverAddress, int framerate, int tickrate, int timeout, int fillSize,
            String username, ModelloGioco modello, Comando riferimentoComando)
    {
        super(serverAddress, framerate, tickrate, timeout);
        proprioUsername = username;
        comandoCorrente = riferimentoComando;
        modelloGioco = modello;
        this.fillSize = fillSize;
    }
        
}
