package xeviousvs;

import java.io.Serializable;

public class PortaAscoltoClient implements Serializable {

    public int porta;
    public int portaMinima;
    public int portaMassima;

    public PortaAscoltoClient(int porta, int portaMinima, int portaMassima) {
        this.porta = porta;
        this.portaMinima = portaMinima;
        this.portaMassima = portaMassima;
    }
}
