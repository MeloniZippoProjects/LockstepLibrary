package xeviousvs;

import java.util.*;
import javafx.geometry.Insets;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

public class VistaGioco {

    private final VBox contenitoreGioco;

    private final PannelloViteUtente pannelloViteGiocatore;
    private final PannelloViteUtente pannelloViteAvversario;
    private final Pane vistaAreaGioco;
    private Navicella navicellaGiocatore;
    private Navicella navicellaAvversario;
    private ArrayList<Proiettile> proiettiliGiocatore;
    private ArrayList<Proiettile> proiettiliAvversario;
    private ModelloGioco modelloGioco;

    private static final Color coloreAreaGioco = Color.LIGHTGREEN;
    private static final Color coloreBordi = Color.BLACK;
    private static final Color colorePannelloAvversario = Color.LIGHTSKYBLUE;
    private static final Color colorePannelloGiocatore = Color.LIGHTCORAL;
    private static final int altezzaPannelli = 30;
    private static final int raggioSpigoli = 5;

    public VistaGioco(VBox contenitoreGioco) {
        this.contenitoreGioco = contenitoreGioco;
        this.contenitoreGioco.setBorder(new Border(new BorderStroke(coloreBordi, BorderStrokeStyle.SOLID, new CornerRadii(raggioSpigoli), BorderStroke.MEDIUM)));

        HBox vistaPannelloViteAvversario = new HBox();
        vistaPannelloViteAvversario.setPrefHeight(altezzaPannelli);
        vistaPannelloViteAvversario.setBackground(new Background(new BackgroundFill(colorePannelloAvversario, new CornerRadii(raggioSpigoli), new Insets(0))));
        vistaPannelloViteAvversario.setBorder(new Border(new BorderStroke(coloreBordi, BorderStrokeStyle.SOLID, new CornerRadii(raggioSpigoli), BorderStroke.THIN)));
        this.pannelloViteAvversario = new PannelloViteUtente(vistaPannelloViteAvversario, ModelloGioco.massimoVite);

        HBox vistaPannelloViteGiocatore = new HBox();
        vistaPannelloViteGiocatore.setPrefHeight(altezzaPannelli);
        vistaPannelloViteGiocatore.setBackground(new Background(new BackgroundFill(colorePannelloGiocatore, new CornerRadii(raggioSpigoli), new Insets(0))));
        vistaPannelloViteGiocatore.setBorder(new Border(new BorderStroke(coloreBordi, BorderStrokeStyle.SOLID, new CornerRadii(raggioSpigoli), BorderStroke.THIN)));
        this.pannelloViteGiocatore = new PannelloViteUtente(vistaPannelloViteGiocatore, ModelloGioco.massimoVite);

        this.vistaAreaGioco = new Pane();
        this.vistaAreaGioco.setPrefSize(this.contenitoreGioco.getPrefWidth(), this.contenitoreGioco.getPrefHeight() - 2 * altezzaPannelli);
        this.vistaAreaGioco.setBackground(new Background(new BackgroundFill(coloreAreaGioco, new CornerRadii(raggioSpigoli), new Insets(0))));

        this.contenitoreGioco.getChildren().addAll(vistaPannelloViteAvversario, this.vistaAreaGioco, vistaPannelloViteGiocatore);
    }

    public void assegnaModelloGioco(ModelloGioco modelloGioco) {
        this.modelloGioco = modelloGioco;
        resetVistaGioco();      //1
    }

    public void resetVistaGioco() {
        if (this.proiettiliGiocatore == null) {
            this.proiettiliGiocatore = new ArrayList<Proiettile>();
        }
        if (this.proiettiliAvversario == null) {
            this.proiettiliAvversario = new ArrayList<Proiettile>();
        }
        
        for(Proiettile proiettile : this.proiettiliGiocatore)
        {
            proiettile.disattivaControlloImpatto();
        }
        for(Proiettile proiettile : this.proiettiliAvversario)
        {
            proiettile.disattivaControlloImpatto();
        }
        
        this.proiettiliGiocatore.clear();
        this.proiettiliAvversario.clear();
       
        this.navicellaGiocatore = new Navicella(Fazione.Giocatore, this.modelloGioco, this, vistaAreaGioco.getPrefWidth() / 2, vistaAreaGioco.getPrefHeight() - Navicella.altezza);
        this.navicellaAvversario = new Navicella(Fazione.Avversario, this.modelloGioco, this, vistaAreaGioco.getPrefWidth() / 2, Navicella.altezza);

        this.vistaAreaGioco.getChildren().clear();
        this.vistaAreaGioco.getChildren().addAll(this.navicellaGiocatore, this.navicellaAvversario);
    }

    public void eseguiAnimazioni() {
        this.navicellaGiocatore.eseguiAnimazione();
        this.navicellaAvversario.eseguiAnimazione();

        for (Proiettile proiettile : this.proiettiliGiocatore) {
            proiettile.eseguiAnimazione();
        }

        for (Proiettile proiettile : this.proiettiliAvversario) {
            proiettile.eseguiAnimazione();
        }
    }

    public void sospendiAnimazioni() {
        this.navicellaGiocatore.sospendiAnimazione();
        this.navicellaAvversario.sospendiAnimazione();

        for (Proiettile proiettile : this.proiettiliGiocatore) {
            proiettile.sospendiAnimazione();
        }

        for (Proiettile proiettile : this.proiettiliAvversario) {
            proiettile.sospendiAnimazione();
        }
    }

    public Pane ottieniVistaAreaGioco() {
        return this.vistaAreaGioco;
    }

    public PannelloViteUtente ottieniPannelloVite(Fazione fazione) {
        switch (fazione) {
            case Giocatore:
                return this.pannelloViteGiocatore;
            case Avversario:
                return this.pannelloViteAvversario;
        }

        return null;
    }

    public Navicella ottieniNavicella(Fazione fazione) {
        switch (fazione) {
            case Giocatore:
                return this.navicellaGiocatore;
            case Avversario:
                return this.navicellaAvversario;
        }

        return null;
    }

    public ArrayList<Proiettile> ottieniProiettili(Fazione fazione) {
        switch (fazione) {
            case Giocatore:
                return this.proiettiliGiocatore;
            case Avversario:
                return this.proiettiliAvversario;
        }

        return null;
    }

    void impostaNavicella(Fazione fazione, double centroX, double centroY) {
        switch (fazione) {
            case Giocatore:
                this.vistaAreaGioco.getChildren().remove(this.navicellaGiocatore);
                this.navicellaGiocatore = new Navicella(Fazione.Giocatore, this.modelloGioco, this, centroX, centroY);
                this.vistaAreaGioco.getChildren().add(this.navicellaGiocatore);
                break;
            case Avversario:
                this.vistaAreaGioco.getChildren().remove(this.navicellaAvversario);
                this.navicellaAvversario = new Navicella(Fazione.Avversario, this.modelloGioco, this, centroX, centroY);
                this.vistaAreaGioco.getChildren().add(this.navicellaAvversario);
                break;
        }
    }

    void aggiungiProiettile(Fazione fazione, double centroX, double centroY) {
        Proiettile proiettile = new Proiettile(fazione, this.modelloGioco, this, centroX, centroY);
        this.vistaAreaGioco.getChildren().add(proiettile);

        switch (fazione) {
            case Giocatore:
                this.proiettiliGiocatore.add(proiettile);
                break;
            case Avversario:
                this.proiettiliAvversario.add(proiettile);
                break;
        }
    }

    void rimuoviProiettile(Proiettile proiettile) {
        proiettile.disattivaControlloImpatto();
        this.vistaAreaGioco.getChildren().remove(proiettile);

        switch (proiettile.ottieniFazione()) {
            case Giocatore:
                this.proiettiliGiocatore.remove(proiettile);
                break;
            case Avversario:
                this.proiettiliAvversario.remove(proiettile);
                break;
        }
    }
}

/*
1: è necessario attendere l'esecuzione di questo metodo per eseguire il primo reset poiché gli elementi di gioco richiedono il modello come argomento del costruttore
*/