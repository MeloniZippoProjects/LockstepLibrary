package xeviousvs;

import java.io.Serializable;

public class DatiCache implements Serializable {

    public String username;
    public DatiCachePartita datiPartita;

    public DatiCache(String username, DatiCachePartita datiPartita) {
        this.username = username;
        this.datiPartita = datiPartita;
    }
}
