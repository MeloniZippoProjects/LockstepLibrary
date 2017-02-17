/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package xeviousvs.client;
import java.net.InetSocketAddress;
import javafx.application.Platform;
import lockstep.*;
import org.apache.log4j.Logger;
import xeviousvs.Comando;
import xeviousvs.Comando.EnumComando;

/**
 *
 * @author enric
 */
public class XeviousLockstepClient extends LockstepClient<Comando>{

    
    public final Comando comandoCorrente;
    public final String usernameProprio;
    public String usernameAvversario;
    public final ModelloGioco modelloGioco;
    
    private final int fillSize;
    
    private static final Logger LOG = Logger.getLogger(XeviousLockstepClient.class.getName());
    private final XeviousVS_Client interfacciaClient;
    
    @Override
    protected Comando readInput() {
        Comando toRet;
        synchronized(comandoCorrente)
        {
            toRet = new Comando(comandoCorrente);
            //toRet = new Comando(EnumComando.Presentazione, usernameProprio);
            comandoCorrente.reset();
        }
        return toRet;
    }

    @Override
    protected void suspendSimulation() {
        Platform.runLater( () -> modelloGioco.vistaGioco.sospendiAnimazioni() );
    }

    @Override
    protected void resumeSimulation() {
        Platform.runLater( () -> modelloGioco.vistaGioco.eseguiAnimazioni() );
    }

    @Override
    protected void executeCommand(Comando c) {
        LOG.debug("Comando da eseguire di tipo " + c.comando.toString());
        if(c.comando == EnumComando.Presentazione && !c.username.equals(usernameProprio) )
        {
            LOG.debug("Ricevuto username avversario: " + c.username);
            usernameAvversario = c.username;
            Platform.runLater(() -> {
                modelloGioco.impostaUsernameAvversario(usernameAvversario);
                modelloGioco.avviaPartita();
            });
        }
        else 
        {
            if(c.username.equals(usernameProprio))
                Platform.runLater( () -> modelloGioco.eseguiComandoGiocatore(c) );
            else if(c.username.equals(usernameAvversario))
                Platform.runLater( () -> modelloGioco.eseguiComandoAvversario(c) );
            else
                LOG.fatal("comando.username non valido");
        }
    }

    @Override
    protected Comando[] fillCommands() {
        Comando[] fillers = new Comando[fillSize];
        
        for(int i = 0; i < fillers.length; ++i)
            fillers[i] = new Comando(EnumComando.NOP, usernameProprio);
        
        return fillers;
    }

    @Override
    protected Comando[] bootstrapCommands() {
        Comando[] fillers = new Comando[1];
        fillers[0] = new Comando(EnumComando.Presentazione, usernameProprio);        
        for(int i = 1; i < fillers.length; ++i)
            fillers[i] = new Comando(EnumComando.NOP, usernameProprio);
        
        return fillers;
    }
    
    public XeviousLockstepClient(InetSocketAddress serverAddress, int framerate, int tickrate, int timeout, int fillSize,
            String username, XeviousVS_Client interfaccia, ModelloGioco modello, Comando riferimentoComando)
    {
        super(serverAddress, framerate, tickrate, timeout);
        usernameProprio = username;
        comandoCorrente = riferimentoComando;
        interfacciaClient = interfaccia;
        modelloGioco = modello;
        this.fillSize = fillSize;
    }
        
}
