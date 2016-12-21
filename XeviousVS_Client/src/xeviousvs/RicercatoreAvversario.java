package xeviousvs;

import java.io.*;
import java.net.*;
import java.util.*;
import javafx.application.Platform;

public class RicercatoreAvversario {

    private int portaAscolto;
    private final XeviousVS interfaccia;
    private final ModelloGioco modelloGioco;
    private TipoRicerca tipoRicerca;

    private RicevitoreConnessioneAvversario ricevitoreConnessione;
    private Socket connessioneAvversario;

    private static final int attesaChiusuraConnessioneMillisecondi = 500;

    public RicercatoreAvversario(int portaAscolto, XeviousVS interfaccia, ModelloGioco modelloGioco) {
        this.portaAscolto = portaAscolto;
        this.interfaccia = interfaccia;
        this.modelloGioco = modelloGioco;
        this.tipoRicerca = TipoRicerca.NessunaRicerca;
    }

    public TipoRicerca ottieniTipoRicerca() {
        return this.tipoRicerca;
    }

    synchronized public void avviaRicercaNuovaPartita() {
        this.tipoRicerca = TipoRicerca.NuovaPartita;
        RegistrazioneUtenteInAscolto reg = OperazioniDatabase.ottieniRicercaPartita();
        if (reg != null) {
            try {
                this.connessioneAvversario = new Socket(reg.indirizzoAscolto, reg.portaAscolto);

                DataOutputStream dout = new DataOutputStream(connessioneAvversario.getOutputStream());
                dout.writeUTF(interfaccia.ottieniUsernameGiocatore());

                this.modelloGioco.resetPartita();
                this.inizializzaConnessioneComandi();
                this.modelloGioco.impostaUsernameAvversario(reg.username);
                this.modelloGioco.avviaPartita();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                this.ricevitoreConnessione = new RicevitoreConnessioneAvversario(this.portaAscolto, this);
                this.ricevitoreConnessione.start();
                RegistrazioneUtenteInAscolto registrazione = new RegistrazioneUtenteInAscolto(interfaccia.ottieniUsernameGiocatore(), InetAddress.getLocalHost().getHostAddress(), this.portaAscolto);
                OperazioniDatabase.registraRicercaPartita(registrazione);
                this.modelloGioco.resetPartita();
                this.interfaccia.impostaBottoneAnnulla(true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    synchronized public void avviaRecuperoPartita() {
        this.tipoRicerca = TipoRicerca.RecuperoPartita;
        RegistrazioneUtenteInAscolto reg = OperazioniDatabase.ottieniRecuperoPartita(this.modelloGioco.ottieniUsernameAvversario());
        if (reg != null) {
            try {
                this.connessioneAvversario = new Socket(reg.indirizzoAscolto, reg.portaAscolto);
                this.inizializzaConnessioneComandi();
                this.modelloGioco.avviaPartita();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                this.ricevitoreConnessione = new RicevitoreConnessioneAvversario(this.portaAscolto, this);
                this.ricevitoreConnessione.start();
                RegistrazioneUtenteInAscolto registrazione = new RegistrazioneUtenteInAscolto(this.interfaccia.ottieniUsernameGiocatore(), InetAddress.getLocalHost().getHostAddress(), this.portaAscolto);
                OperazioniDatabase.registraRecuperoPartita(registrazione);
                this.interfaccia.impostaBottoneAnnulla(true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    synchronized public void interrompiRicerca() {
        switch (this.tipoRicerca) {
            case NuovaPartita:
                this.interrompiRicercaNuovaPartita();
                break;
            case RecuperoPartita:
                this.interrompiRecuperoPartita();
                break;
        }
    }

    synchronized private void interrompiRicercaNuovaPartita() {
        this.tipoRicerca = TipoRicerca.NessunaRicerca;
        OperazioniDatabase.rimuoviRicercaPartita(this.interfaccia.ottieniUsernameGiocatore());
        this.ricevitoreConnessione.interrupt();
        try {
            this.ricevitoreConnessione.join();
            if (this.ricevitoreConnessione.ottieniStato() == StatoRicevitoreConnessione.TerminatoSenzaConnessione) {
                this.interfaccia.impostaMessaggio(XeviousVS.messaggioAttesaInterrotta);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    synchronized private void interrompiRecuperoPartita() {
        this.tipoRicerca = TipoRicerca.NessunaRicerca;
        OperazioniDatabase.rimuoviRecuperoPartita(this.interfaccia.ottieniUsernameGiocatore());
        this.ricevitoreConnessione.interrupt();
        try {
            this.ricevitoreConnessione.join();
            if (this.ricevitoreConnessione.ottieniStato() == StatoRicevitoreConnessione.TerminatoSenzaConnessione) {
                this.interfaccia.impostaMessaggio(XeviousVS.messaggioAttesaInterrotta);

                this.modelloGioco.resetPartita();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    synchronized public void riceviConnessione(Socket connessioneAvversario) {
        this.connessioneAvversario = connessioneAvversario;

        if (this.tipoRicerca == TipoRicerca.NuovaPartita) {
            OperazioniDatabase.rimuoviRicercaPartita(this.interfaccia.ottieniUsernameGiocatore());

            try {
                DataInputStream din = new DataInputStream(this.connessioneAvversario.getInputStream());
                String usernameAvversario = din.readUTF();
                this.modelloGioco.impostaUsernameAvversario(usernameAvversario);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            OperazioniDatabase.rimuoviRecuperoPartita(this.interfaccia.ottieniUsernameGiocatore());
        }

        inizializzaConnessioneComandi();
        this.portaAscolto++;            //1
        
        Platform.runLater(() -> {
            this.interfaccia.impostaBottoneGioca(false);
            this.modelloGioco.avviaPartita();
        });
    }

    synchronized private void inizializzaConnessioneComandi() {
        RicevitoreComandi ricevitore = new RicevitoreComandi(this.connessioneAvversario, this.modelloGioco);
        TrasmettitoreComandi trasmettitore = new TrasmettitoreComandi(this.connessioneAvversario);
        this.interfaccia.impostaConnessionePartita(ricevitore, trasmettitore);
        ricevitore.start();

        if (this.tipoRicerca == TipoRicerca.RecuperoPartita) {
            LoggerEventoXml.registraEvento(LoggerEventoXml.descrizioneEventoRecuperoPartita);
        }
        this.tipoRicerca = TipoRicerca.NessunaRicerca;
    }

    synchronized public void chiudiConnessioneAvversario(boolean attendiPropagazione) {
        if (this.connessioneAvversario != null) {
            if (attendiPropagazione) {
                Timer timer = new Timer(true);
                TimerTask task = new TimerTask() {
                    public void run() {
                        try {
                            RicercatoreAvversario.this.connessioneAvversario.close();
                            RicercatoreAvversario.this.connessioneAvversario = null;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                };
                timer.schedule(task, RicercatoreAvversario.attesaChiusuraConnessioneMillisecondi);
            } else {
                try {
                    RicercatoreAvversario.this.connessioneAvversario.close();
                    RicercatoreAvversario.this.connessioneAvversario = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

/*
1: è possibile che una richiesta di un socket di ascolto su una porta fallisca a causa delle attività svolte sulla porta precedentemente.
    Per evitare errori per richieste troppo vicine temporalmente fra loro, ad ogni connessione ricevuta si cambia la porta di ascolto da utilizzare per le successive ricerche
*/