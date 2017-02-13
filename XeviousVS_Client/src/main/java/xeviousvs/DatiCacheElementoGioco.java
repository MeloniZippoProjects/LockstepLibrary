package xeviousvs;

import java.io.Serializable;

public class DatiCacheElementoGioco implements Serializable {

    public Fazione fazione;
    public double centroX;
    public double centroY;

    public DatiCacheElementoGioco(Fazione fazione, double centroX, double centroY) {
        this.fazione = fazione;
        this.centroX = centroX;
        this.centroY = centroY;
    }
}
