package xeviousvs;

public class ModelloGioco implements java.io.Serializable {

    private int viteGiocatore;
    private int viteAvversario;
    private String usernameAvversario;

    private StatoPartita statoPartita;
    private VistaGioco vistaGioco;
    private final XeviousVS interfaccia;
    public final static int massimoVite = 5;

    public ModelloGioco(XeviousVS interfaccia) {
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

    public synchronized void impostaRecuperoPartita(String usernameAvversario, int viteGiocatore, int viteAvversario) {
        this.usernameAvversario = usernameAvversario;
        this.viteGiocatore = viteGiocatore;
        this.viteAvversario = viteAvversario;

        this.vistaGioco.ottieniPannelloVite(Fazione.Giocatore).aggiornaVistaVite(this.viteGiocatore);
        this.vistaGioco.ottieniPannelloVite(Fazione.Avversario).aggiornaVistaVite(this.viteAvversario);
        
        this.statoPartita = StatoPartita.InAttesaRecuperoConnessione;
    }

    public synchronized void impostaUsernameAvversario(String usernameAvversario) {
        this.usernameAvversario = usernameAvversario;
    }

    public synchronized String ottieniUsernameAvversario() {
        return this.usernameAvversario;
    }

    public synchronized void eseguiComandoGiocatore(xeviousvs.Comando comando) {
        switch (this.statoPartita) {
            case InAttesaUtenti:
                if (comando == Comando.Fuoco) {
                    this.statoPartita = StatoPartita.InAttesaAvversario;
                    this.interfaccia.impostaMessaggio(XeviousVS.messaggioAttesaAvversario.replace(XeviousVS.segnapostoSostituizioniMessaggi, usernameAvversario));
                }
                break;
            case InAttesaGiocatore:
                if (comando == Comando.Fuoco) {
                    this.statoPartita = StatoPartita.Attiva;
                    this.interfaccia.impostaMessaggio(XeviousVS.messaggioPartitaInCorso);
                    this.vistaGioco.eseguiAnimazioni();
                }
                break;
            case InPausaDaGiocatore:
                if (comando == Comando.Fuoco) {
                    this.statoPartita = StatoPartita.Attiva;
                    this.interfaccia.impostaMessaggio(XeviousVS.messaggioPartitaInCorso);
                    this.vistaGioco.eseguiAnimazioni();
                    LoggerEventoXml.registraEvento(LoggerEventoXml.descrizioneEventoRipresaPartita);
                }
                break;
            case Attiva:
                eseguiComandoPartitaAttiva(comando, Fazione.Giocatore);
        }
    }

    public synchronized void eseguiComandoAvversario(xeviousvs.Comando comando) {
        switch (this.statoPartita) {
            case InAttesaUtenti:
                if (comando == Comando.Fuoco) {
                    this.statoPartita = StatoPartita.InAttesaGiocatore;
                }
                break;
            case InAttesaAvversario:
                if (comando == Comando.Fuoco) {
                    this.statoPartita = StatoPartita.Attiva;
                    this.interfaccia.impostaMessaggio(XeviousVS.messaggioPartitaInCorso);
                    this.vistaGioco.eseguiAnimazioni();
                }
                break;
            case InPausaDaAvversario:
                if (comando == Comando.Fuoco) {
                    this.statoPartita = StatoPartita.Attiva;
                    this.interfaccia.impostaMessaggio(XeviousVS.messaggioPartitaInCorso);
                    this.vistaGioco.eseguiAnimazioni();
                }
                break;
            case Attiva:
                eseguiComandoPartitaAttiva(comando, Fazione.Avversario);
        }
    }

    private synchronized void eseguiComandoPartitaAttiva(Comando comando, Fazione fazione) {
        switch (comando) {
            case Destra:
                this.vistaGioco.ottieniNavicella(fazione).spostaDestra();
                break;
            case Sinistra:
                this.vistaGioco.ottieniNavicella(fazione).spostaSinistra();
                break;
            case Fuoco:
                this.vistaGioco.ottieniNavicella(fazione).sparaProiettile();
                break;
            case Pausa:
                if (fazione == Fazione.Giocatore) {
                    this.statoPartita = StatoPartita.InPausaDaGiocatore;
                    this.interfaccia.impostaMessaggio(XeviousVS.messaggioPartitaInPausaGiocatore);
                    this.vistaGioco.sospendiAnimazioni();
                    LoggerEventoXml.registraEvento(LoggerEventoXml.descrizioneEventoPausaPartita);
                } else {
                    this.statoPartita = StatoPartita.InPausaDaAvversario;
                    this.interfaccia.impostaMessaggio(XeviousVS.messaggioPartitaInPausaAvversario.replace(XeviousVS.segnapostoSostituizioniMessaggi, usernameAvversario));
                    this.vistaGioco.sospendiAnimazioni();
                }
        }
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
        this.interfaccia.impostaMessaggio(XeviousVS.messaggioPartitaPronta.replace(XeviousVS.segnapostoSostituizioniMessaggi, usernameAvversario));
        LoggerEventoXml.registraEvento(LoggerEventoXml.descrizioneEventoAvvioPartita);
    }

    public synchronized void interrompiPartita() {
        this.vistaGioco.sospendiAnimazioni();
        this.statoPartita = StatoPartita.InAttesaRecuperoConnessione;
        LoggerEventoXml.registraEvento(LoggerEventoXml.descrizioneEventoInterruzionePartita);
        this.interfaccia.impostaRecuperoPartita();
    }

    private synchronized void terminaPartita(boolean vittoria) {
        this.vistaGioco.sospendiAnimazioni();
        this.statoPartita = StatoPartita.Inattiva;
        if (vittoria) {
            this.interfaccia.impostaMessaggio(XeviousVS.messaggioVittoria.replace(XeviousVS.segnapostoSostituizioniMessaggi, usernameAvversario));
        } else {
            this.interfaccia.impostaMessaggio(XeviousVS.messaggioSconfitta.replace(XeviousVS.segnapostoSostituizioniMessaggi, usernameAvversario));
        }
        this.interfaccia.impostaTerminazionePartita(vittoria);
    }
}
