package xeviousvs;

import java.io.IOException;
import java.net.*;

public class RicevitoreConnessioneAvversario extends Thread {

    private StatoRicevitoreConnessione stato;
    private final int portaAscolto;
    private final RicercatoreAvversario ricercatoreAvversario;

    private static final int intervalloAscoltoMillisecondi = 400;

    public RicevitoreConnessioneAvversario(int portaAscolto, RicercatoreAvversario ricercatoreAvversario) {
        this.stato = StatoRicevitoreConnessione.InAttesa;
        this.portaAscolto = portaAscolto;
        this.ricercatoreAvversario = ricercatoreAvversario;
        this.setName("Ricevitore connessione su porta " + portaAscolto);
    }

    public StatoRicevitoreConnessione ottieniStato() {
        return this.stato;
    }

    @Override
    public void run() {
        ServerSocket socketAttesa;
        try {
            socketAttesa = new ServerSocket(this.portaAscolto);
            socketAttesa.setReuseAddress(true);                         //1
            socketAttesa.setSoTimeout(intervalloAscoltoMillisecondi);

            while (true) {
                try {
                    Socket connessioneAvversario = socketAttesa.accept();
                    socketAttesa.close();
                    this.ricercatoreAvversario.riceviConnessione(connessioneAvversario);
                    this.stato = StatoRicevitoreConnessione.TerminatoConConnessione;
                    return;
                } catch (SocketTimeoutException e) {                    //2
                    if (this.isInterrupted()) {
                        this.stato = StatoRicevitoreConnessione.TerminatoSenzaConnessione;
                        socketAttesa.close();
                        return;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

/*
1: minimizza i casi in cui due esecuzioni successive dell'applicazione portano all'impossibilità di acquisire il socket in ascolto
    https://docs.oracle.com/javase/7/docs/api/java/net/ServerSocket.html#setReuseAddress(boolean)
*/
