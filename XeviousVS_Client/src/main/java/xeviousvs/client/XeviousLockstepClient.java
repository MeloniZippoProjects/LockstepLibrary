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
public class XeviousLockstepClient extends LockstepClient<Comando>
{

    public final Comando comandoCorrente;
    public final String usernameProprio;
    public String usernameAvversario;
    public final ModelloGioco modelloGioco;

    private static final int tempoAttesaFuocoMillisecondi = 500;
    private final int fireInterframe;
    private int framesFromLastFire;
    private final int fillSize;

    private static final Logger LOG = Logger.getLogger(XeviousLockstepClient.class.getName());
    private final XeviousVS_Client interfacciaClient;

    public XeviousLockstepClient(InetSocketAddress serverAddress, int framerate, int tickrate, int timeout, int protocolDelay,
            String username, XeviousVS_Client interfaccia, ModelloGioco modello, Comando riferimentoComando)
    {
        super(serverAddress, framerate, tickrate, timeout);
        usernameProprio = username;
        comandoCorrente = riferimentoComando;
        interfacciaClient = interfaccia;
        modelloGioco = modello;
        this.fillSize = (int) Math.ceil(protocolDelay / (1000 / framerate));
        fireInterframe = (int) Math.ceil(tempoAttesaFuocoMillisecondi / (1000 / framerate));
        framesFromLastFire = fireInterframe;
    }

    @Override
    protected Comando readInput()
    {
        Comando toRet;
        synchronized (comandoCorrente)
        {
            toRet = new Comando(comandoCorrente);
            comandoCorrente.reset();
        }

        if (toRet.comando == EnumComando.Fuoco || toRet.comando == EnumComando.FuocoDestra || toRet.comando == EnumComando.FuocoSinistra)
        {
            if (framesFromLastFire < fireInterframe)
            {
                switch (toRet.comando)
                {
                    case Fuoco:
                        toRet.comando = EnumComando.NOP;
                        break;
                    case FuocoDestra:
                        toRet.comando = EnumComando.Destra;
                        break;
                    case FuocoSinistra:
                        toRet.comando = EnumComando.Sinistra;
                        break;
                }
            }
            else
            {
                framesFromLastFire = -1;
            }
        }
        framesFromLastFire++;
        return toRet;
    }

    @Override
    protected void suspendSimulation()
    {
        Platform.runLater(() -> modelloGioco.vistaGioco.sospendiAnimazioni());
    }

    @Override
    protected void resumeSimulation()
    {
        Platform.runLater(() -> modelloGioco.vistaGioco.eseguiAnimazioni());
    }

    @Override
    protected void executeCommand(Comando c)
    {
        LOG.debug("Comando da eseguire di tipo " + c.comando.toString());
        if (c.comando == EnumComando.Presentazione && !c.username.equals(usernameProprio))
        {
            LOG.debug("Ricevuto username avversario: " + c.username);
            usernameAvversario = c.username;
            Platform.runLater(() ->
            {
                modelloGioco.impostaUsernameAvversario(usernameAvversario);
                modelloGioco.avviaPartita();
            });
        }
        else
        {
            if (c.username.equals(usernameProprio))
            {
                Platform.runLater(() -> modelloGioco.eseguiComandoGiocatore(c));
            }
            else if (c.username.equals(usernameAvversario))
            {
                Platform.runLater(() -> modelloGioco.eseguiComandoAvversario(c));
            }
            else
            {
                LOG.fatal("comando.username non valido");
            }
        }
    }

    @Override
    protected Comando[] fillCommands()
    {
        Comando[] fillers = new Comando[0];

        for (int i = 0; i < fillers.length; ++i)
        {
            fillers[i] = new Comando(EnumComando.NOP, usernameProprio);
        }

        return fillers;
    }

    @Override
    protected Comando[] bootstrapCommands()
    {
        Comando[] fillers = new Comando[1];
        fillers[0] = new Comando(EnumComando.Presentazione, usernameProprio);
        for (int i = 1; i < fillers.length; ++i)
        {
            fillers[i] = new Comando(EnumComando.NOP, usernameProprio);
        }

        return fillers;
    }
}
