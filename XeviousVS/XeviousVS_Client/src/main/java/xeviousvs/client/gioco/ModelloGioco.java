package xeviousvs.client.gioco;

import xeviousvs.Comando;
import xeviousvs.Comando.EnumComando;
import xeviousvs.client.LoggerEventoXml;
import xeviousvs.client.XeviousVS_Client;

public class ModelloGioco implements java.io.Serializable {

    private int viteGiocatore;
    private int viteAvversario;
    private String usernameAvversario;

    private StatoPartita statoPartita;
    public VistaGioco vistaGioco;
    private final XeviousVS_Client interfaccia;
    public final static int massimoVite = 5;

    public ModelloGioco(XeviousVS_Client interfaccia) {
        this.interfaccia = interfaccia;
        this.viteGiocatore = massimoVite;
        this.viteAvversario = massimoVite;
        this.statoPartita = StatoPartita.Inattiva;
    }

    public synchronized void assegnaVistaGioco(VistaGioco vistaGioco) {
        this.vistaGioco = vistaGioco;
    }

    public synchronized StatoPartita ottieniStatoPartita() {
        return this.statoPartita;
    }

    public synchronized void impostaUsernameAvversario(String usernameAvversario) {
        this.usernameAvversario = usernameAvversario;
    }

    public synchronized String ottieniUsernameAvversario() {
        return this.usernameAvversario;
    }

    public synchronized void eseguiComandoGiocatore(Comando inputCmd) {
        switch (this.statoPartita) {
            case InAttesaUtenti:
                if (inputCmd.comando == EnumComando.Fuoco) {
                    this.statoPartita = StatoPartita.InAttesaAvversario;
                    this.interfaccia.impostaMessaggio(XeviousVS_Client.MSG_ATTESA_AVVERSARIO.replace(XeviousVS_Client.MSG_PLACEHOLDER, usernameAvversario));
                }
                break;
            case InAttesaGiocatore:
                if (inputCmd.comando == EnumComando.Fuoco) {
                    this.statoPartita = StatoPartita.Attiva;
                    this.interfaccia.impostaMessaggio(XeviousVS_Client.MSG_PARTITA_IN_CORSO);
                    this.vistaGioco.eseguiAnimazioni();
                }
                break;
            case InPausaDaGiocatore:
                if (inputCmd.comando == EnumComando.Fuoco) {
                    this.statoPartita = StatoPartita.Attiva;
                    this.interfaccia.impostaMessaggio(XeviousVS_Client.MSG_PARTITA_IN_CORSO);
                    this.vistaGioco.eseguiAnimazioni();
                    LoggerEventoXml.registraEvento(LoggerEventoXml.LOG_PARTITA_RIPRESA);
                }
                break;
            case Attiva:
                eseguiComandoPartitaAttiva(inputCmd, Fazione.Giocatore);
        }
    }

    public synchronized void eseguiComandoAvversario(Comando inputCmd) {
        switch (this.statoPartita) {
            case InAttesaUtenti:
                if (inputCmd.comando == EnumComando.Fuoco) {
                    this.statoPartita = StatoPartita.InAttesaGiocatore;
                }
                break;
            case InAttesaAvversario:
                if (inputCmd.comando == EnumComando.Fuoco) {
                    this.statoPartita = StatoPartita.Attiva;
                    this.interfaccia.impostaMessaggio(XeviousVS_Client.MSG_PARTITA_IN_CORSO);
                    this.vistaGioco.eseguiAnimazioni();
                }
                break;
            case InPausaDaAvversario:
                if (inputCmd.comando == EnumComando.Fuoco) {
                    this.statoPartita = StatoPartita.Attiva;
                    this.interfaccia.impostaMessaggio(XeviousVS_Client.MSG_PARTITA_IN_CORSO);
                    this.vistaGioco.eseguiAnimazioni();
                }
                break;
            case Attiva:
                eseguiComandoPartitaAttiva(inputCmd, Fazione.Avversario);
        }
    }

    private synchronized void eseguiComandoPartitaAttiva(Comando inputCmd, Fazione fazione) {
        switch (inputCmd.comando) {
            
            case NOP:
                break;
            case FuocoDestra:
                this.vistaGioco.ottieniNavicella(fazione).sparaProiettile();
            case Destra:
                this.vistaGioco.ottieniNavicella(fazione).spostaDestra();
                break;
            case FuocoSinistra:
                this.vistaGioco.ottieniNavicella(fazione).sparaProiettile();
            case Sinistra:
                this.vistaGioco.ottieniNavicella(fazione).spostaSinistra();
                break;
            case Fuoco:
                this.vistaGioco.ottieniNavicella(fazione).sparaProiettile();
                break;
            case Pausa:
                if (fazione == Fazione.Giocatore) {
                    this.statoPartita = StatoPartita.InPausaDaGiocatore;
                    this.interfaccia.impostaMessaggio(XeviousVS_Client.MSG_PARTITA_IN_PAUSA_GIOCATORE);
                    this.vistaGioco.sospendiAnimazioni();
                    LoggerEventoXml.registraEvento(LoggerEventoXml.LOG_PARTITA_PAUSA);
                } else {
                    this.statoPartita = StatoPartita.InPausaDaAvversario;
                    this.interfaccia.impostaMessaggio(XeviousVS_Client.MSG_PARTITA_IN_PAUSA_AVVERSARIO.replace(XeviousVS_Client.MSG_PLACEHOLDER, usernameAvversario));
                    this.vistaGioco.sospendiAnimazioni();
                }
        }
        if(fazione == Fazione.Giocatore)
            vistaGioco.eseguiFrameProiettili();
    }

    public synchronized void rimuoviVita(Fazione fazione) {
        switch (fazione) {
            case Giocatore:
                this.viteGiocatore--;
                this.vistaGioco.ottieniPannelloVite(Fazione.Giocatore).aggiornaVistaVite(this.viteGiocatore);
                if (this.viteGiocatore == 0) {
                    this.terminaPartita(false);
                }
                break;
            case Avversario:
                this.viteAvversario--;
                this.vistaGioco.ottieniPannelloVite(Fazione.Avversario).aggiornaVistaVite(this.viteAvversario);
                if (this.viteAvversario == 0) {
                    this.terminaPartita(true);
                }
                break;
        }
    }

    public synchronized int ottieniVite(Fazione fazione) {
        switch (fazione) {
            case Giocatore:
                return this.viteGiocatore;
            case Avversario:
                return this.viteAvversario;
        }
        return 0;
    }

    public synchronized void resetPartita() {
        this.viteGiocatore = massimoVite;
        this.viteAvversario = massimoVite;

        this.vistaGioco.ottieniPannelloVite(Fazione.Giocatore).aggiornaVistaVite(massimoVite);
        this.vistaGioco.ottieniPannelloVite(Fazione.Avversario).aggiornaVistaVite(massimoVite);
        this.vistaGioco.resetVistaGioco();
    }

    public synchronized void avviaPartita() {
        this.statoPartita = StatoPartita.InAttesaUtenti;
        this.interfaccia.impostaMessaggio(XeviousVS_Client.MSG_PARTITA_PRONTA.replace(XeviousVS_Client.MSG_PLACEHOLDER, usernameAvversario));
        LoggerEventoXml.registraEvento(LoggerEventoXml.LOG_PARTITA_AVVIATA);
    }

    public synchronized void interrompiPartita() {
        this.vistaGioco.sospendiAnimazioni();
    }

    private synchronized void terminaPartita(boolean vittoria) {
        this.vistaGioco.sospendiAnimazioni();
        this.statoPartita = StatoPartita.Inattiva;
        if (vittoria) {
            this.interfaccia.impostaMessaggio(XeviousVS_Client.MSG_VITTORIA.replace(XeviousVS_Client.MSG_PLACEHOLDER, usernameAvversario));
        } else {
            this.interfaccia.impostaMessaggio(XeviousVS_Client.MSG_SCONFITTA.replace(XeviousVS_Client.MSG_PLACEHOLDER, usernameAvversario));
        }
        this.interfaccia.impostaTerminazionePartita(vittoria);
    }
}
