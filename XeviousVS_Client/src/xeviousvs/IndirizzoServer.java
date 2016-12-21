package xeviousvs;

import java.io.Serializable;

public class IndirizzoServer implements Serializable {

    public String indirizzoIP;
    public int porta;

    public IndirizzoServer(String indirizzoIP, int porta) {
        this.indirizzoIP = indirizzoIP;
        this.porta = porta;
    }

}
