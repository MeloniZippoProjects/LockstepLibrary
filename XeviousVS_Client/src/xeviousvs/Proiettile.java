package xeviousvs;

import java.util.*;
import javafx.scene.shape.*;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import javafx.animation.TranslateTransition;
import javafx.event.ActionEvent;

public class Proiettile extends Circle {

    private final TranslateTransition transizioneAttiva;
    private Fazione fazione;
    private final RilevatoreSpostamento rilevatoreSpostamento;
    private final ModelloGioco modelloGioco;
    private VistaGioco vistaGioco;

    public static final int diametro = 16;
    private static final Color coloreInterno = Color.YELLOW;
    private static final Color coloreBordo = Color.ORANGE;
    private static final double spessoreBordo = 3;
    private static final int tempoAttraversamentoAreaGiocoMillisecondi = 1500;

    public Proiettile(Fazione fazione, ModelloGioco modelloGioco, VistaGioco vistaGioco, double centroX, double centroY) {
        super(centroX, centroY, diametro / 2);

        this.fazione = fazione;
        this.modelloGioco = modelloGioco;
        this.vistaGioco = vistaGioco;

        this.setStroke(coloreBordo);
        this.setStrokeWidth(spessoreBordo);

        this.setFill(coloreInterno);

        double posizioneObiettivoY = (this.fazione == Fazione.Giocatore)
                ? this.getLayoutY() - (this.getBoundsInParent().getMinY())
                : this.getLayoutY() + this.vistaGioco.ottieniVistaAreaGioco().getPrefHeight() - this.getBoundsInParent().getMinY();

        double durataSpostamento = (Math.abs(posizioneObiettivoY) / this.vistaGioco.ottieniVistaAreaGioco().getPrefHeight()) * Proiettile.tempoAttraversamentoAreaGiocoMillisecondi;

        this.transizioneAttiva = new TranslateTransition(Duration.millis(durataSpostamento), this);
        this.transizioneAttiva.setFromY(this.getLayoutY());
        this.transizioneAttiva.setToY(posizioneObiettivoY);

        this.transizioneAttiva.setOnFinished((ActionEvent event) -> {
            this.vistaGioco.ottieniVistaAreaGioco().getChildren().remove(this);
            this.vistaGioco.ottieniProiettili(this.fazione).remove(this);
        });

        this.rilevatoreSpostamento = new RilevatoreSpostamento(this);
        this.boundsInParentProperty().addListener(this.rilevatoreSpostamento);
    }

    public Fazione ottieniFazione() {
        return this.fazione;
    }

    public void eseguiAnimazione() {
        if (this.transizioneAttiva != null) {
            this.transizioneAttiva.play();
        }
    }

    public void sospendiAnimazione() {
        if (this.transizioneAttiva != null) {
            this.transizioneAttiva.pause();
        }
    }
    
    public void disattivaControlloImpatto()
    {
        this.boundsInParentProperty().removeListener(rilevatoreSpostamento);
    }
    
    public void controllaImpatto() {        //1
        if (!this.controllaImpattoProiettili()) {
            this.controllaImpattoNavicella();
        }
    }

    private boolean controllaImpattoProiettili() {
        boolean eliminato = false;
        Fazione fazioneOpposta = (fazione == Fazione.Giocatore) ? Fazione.Avversario : Fazione.Giocatore;
        Iterator<Proiettile> iterator = this.vistaGioco.ottieniProiettili(fazioneOpposta).iterator();
        while (iterator.hasNext() && !eliminato) {
            Proiettile obiettivo = iterator.next();
            if (((Path) Shape.intersect(this, obiettivo)).getElements().size() > 0) {
                this.vistaGioco.rimuoviProiettile(obiettivo);
                this.vistaGioco.rimuoviProiettile(this);
                eliminato = true;
                break;
            }
        }

        return eliminato;
    }

    private void controllaImpattoNavicella() {
        Fazione fazioneOpposta = (fazione == Fazione.Giocatore) ? Fazione.Avversario : Fazione.Giocatore;

        if (((Path) Shape.intersect(this, this.vistaGioco.ottieniNavicella(fazioneOpposta))).getElements().size() > 0) {
            this.vistaGioco.rimuoviProiettile(this);
            this.modelloGioco.rimuoviVita(fazioneOpposta);
        }
    }
}

/*
1: Il listener utilizzato rileva come cambiamento della posizione anche la rimozione del proiettile dall'area di gioco, che possono portare a interazioni indeterminate con altri elementi del gioco.
    Pertanto, prima di rimuovere un proiettile dall'area di gioco, è necessario utilizzare questo metodo per rimuoverne il listener.
*/