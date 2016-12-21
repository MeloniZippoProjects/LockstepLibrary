package xeviousvs;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import javafx.application.Platform;

public class RicevitoreComandi extends Thread {

    private final Socket connessioneAvversario;
    private final ModelloGioco modelloGioco;

    public RicevitoreComandi(Socket connessioneAvversario, ModelloGioco modelloGioco) {
        this.connessioneAvversario = connessioneAvversario;
        this.modelloGioco = modelloGioco;
        this.setName("Ricevitore comandi da " + modelloGioco.ottieniUsernameAvversario());
    }

    @Override
    public void run() {
        try {
            ObjectInputStream streamComandiIngresso = new ObjectInputStream(this.connessioneAvversario.getInputStream());
            while (true) {
                Comando comandoAvversario = (Comando) streamComandiIngresso.readObject();
                Platform.runLater(() -> {
                    this.modelloGioco.eseguiComandoAvversario(comandoAvversario);
                });
            }
        } catch (IOException | ClassNotFoundException e) {
            if (!this.isInterrupted() && modelloGioco.ottieniStatoPartita() != StatoPartita.Inattiva) { //1
                Platform.runLater(() -> {
                    modelloGioco.interrompiPartita();
                });
            }
            return;
        }
    }

}

/*
1: l'invio e il controllo del segnale di interruzione permette di riconoscere il caso in cui l'applicazione sta subendo una terminazione e non avviare quindi la procedura di recupero della partita.
    Questo controllo è necessario perché il socket utilizzato viene chiuso prima che il thread termini.
*/