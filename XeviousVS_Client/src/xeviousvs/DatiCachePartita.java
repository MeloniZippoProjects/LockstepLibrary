package xeviousvs;

import java.io.Serializable;
import java.util.ArrayList;

public class DatiCachePartita implements Serializable {

    public String usernameAvversario;
    public int viteGiocatore;
    public int viteAvversario;
    public DatiCacheElementoGioco navicellaGiocatore;
    public DatiCacheElementoGioco navicellaAvversario;
    public ArrayList<DatiCacheElementoGioco> proiettiliGiocatore;
    public ArrayList<DatiCacheElementoGioco> proiettiliAvversario;

    public DatiCachePartita(String usernameAvversario,
            int viteGiocatore,
            int viteAvversario,
            DatiCacheElementoGioco navicellaGiocatore,
            DatiCacheElementoGioco navicellaAvversario,
            ArrayList<DatiCacheElementoGioco> proiettiliGiocatore,
            ArrayList<DatiCacheElementoGioco> proiettiliAvversario) {
        this.usernameAvversario = usernameAvversario;
        this.viteGiocatore = viteGiocatore;
        this.viteAvversario = viteAvversario;
        this.navicellaGiocatore = navicellaGiocatore;
        this.navicellaAvversario = navicellaAvversario;
        this.proiettiliGiocatore = proiettiliGiocatore;
        this.proiettiliAvversario = proiettiliAvversario;
    }
}
