package xeviousvs;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class TrasmettitoreComandi {

    private Socket connessioneAvversario;
    private ObjectOutputStream streamComandiUscita;

    public TrasmettitoreComandi(Socket connessioneAvversario) {
        this.connessioneAvversario = connessioneAvversario;
        try {
            streamComandiUscita = new ObjectOutputStream(this.connessioneAvversario.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void inoltraComando(Comando comando) {
        try {
            streamComandiUscita.writeObject(comando);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
