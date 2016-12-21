package xeviousvs;

import java.util.*;
import javafx.scene.shape.*;
import javafx.scene.paint.Color;
import javafx.animation.TranslateTransition;
import javafx.event.ActionEvent;
import javafx.util.Duration;

public class Navicella extends Path {

    private TranslateTransition transizioneAttiva;
    private final Fazione fazione;
    private final ModelloGioco modelloGioco;
    private final VistaGioco vistaGioco;
    private boolean prontaASparare = true;

    private static final double lato = 38;
    public static final double altezza = (Math.sqrt(3) / 2) * lato;
    private static final double lunghezzaSpostamento = lato / 2;
    private static final int tempoSpostamentoMillisecondi = 100;
    private static final int tempoAttesaFuocoMillisecondi = 500;
    private static final Color coloreNavicellaGiocatore = Color.RED;
    private static final Color coloreNavicellaAvversario = Color.BLUE;
    private static final Color coloreBordo = Color.BLACK;
    private static final double spessoreBordo = 3.5;

    public Navicella(Fazione fazione, ModelloGioco modelloGioco, VistaGioco vistaGioco, double centroX, double centroY) {
        super(
                new MoveTo(centroX - lato / 2, centroY + altezza / 2),
                new LineTo(centroX + lato / 2, centroY + altezza / 2),
                new LineTo(centroX, centroY - altezza / 2),
                new ClosePath()
        );

        this.fazione = fazione;
        this.vistaGioco = vistaGioco;
        this.transizioneAttiva = null;
        this.modelloGioco = modelloGioco;

        this.setStroke(coloreBordo);
        this.setStrokeWidth(spessoreBordo);

        if (fazione == Fazione.Giocatore) {
            this.setFill(coloreNavicellaGiocatore);
        } else {
            this.setFill(coloreNavicellaAvversario);
            this.setRotate(180);
        }
    }

    public synchronized Fazione ottieniFazione() {
        return this.fazione;
    }

    public synchronized void eseguiAnimazione() {
        if (this.transizioneAttiva != null) {
            this.transizioneAttiva.play();
        }
    }

    public synchronized void sospendiAnimazione() {
        if (this.transizioneAttiva != null) {
            this.transizioneAttiva.pause();
        }
    }

    public synchronized void sparaProiettile() {
        if (!this.prontaASparare) {
            return;
        }

        double centroX = this.getBoundsInParent().getMinX() + lato / 2;
        double centroY = (fazione == Fazione.Giocatore) ? this.getBoundsInParent().getMinY() - Proiettile.diametro : this.getBoundsInParent().getMaxY() + Proiettile.diametro;

        Proiettile proiettile = new Proiettile(fazione, modelloGioco, vistaGioco, centroX, centroY);
        this.vistaGioco.ottieniVistaAreaGioco().getChildren().add(proiettile);
        this.vistaGioco.ottieniProiettili(fazione).add(proiettile);

        proiettile.eseguiAnimazione();

        this.prontaASparare = false;
        Timer timer = new Timer(true);
        TimerTask task = new TimerTask() {
            public void run() {
                impostaProntaASparare();
            }
        };
        timer.schedule(task, Navicella.tempoAttesaFuocoMillisecondi);
    }

    private synchronized void impostaProntaASparare() {
        this.prontaASparare = true;
    }

    public synchronized void spostaDestra() {
        switch (this.fazione) {
            case Giocatore:
                this.traslaDestra();
                break;
            case Avversario:
                this.traslaSinistra();
                break;
        }
    }

    public synchronized void spostaSinistra() {
        switch (this.fazione) {
            case Giocatore:
                this.traslaSinistra();
                break;
            case Avversario:
                this.traslaDestra();
                break;
        }
    }

    private synchronized void traslaDestra() {
        if (this.transizioneAttiva == null) {
            double posizioneCorrenteX = this.getLayoutX() + this.getTranslateX();

            if (this.getBoundsInParent().getMaxX() + lunghezzaSpostamento < this.vistaGioco.ottieniVistaAreaGioco().getPrefWidth()) {
                this.transizioneAttiva = new TranslateTransition(Duration.millis(tempoSpostamentoMillisecondi), this);
                this.transizioneAttiva.setFromX(posizioneCorrenteX);
                this.transizioneAttiva.setToX(posizioneCorrenteX + lunghezzaSpostamento);

                this.transizioneAttiva.setOnFinished((ActionEvent event) -> {
                    this.transizioneAttiva = null;
                });
                this.transizioneAttiva.play();
            }
        }
    }

    private synchronized void traslaSinistra() {
        if (this.transizioneAttiva == null) {
            double posizioneCorrenteX = this.getLayoutX() + this.getTranslateX();

            if (this.getBoundsInParent().getMinX() - lunghezzaSpostamento > 0) {
                this.transizioneAttiva = new TranslateTransition(Duration.millis(tempoSpostamentoMillisecondi), this);
                this.transizioneAttiva.setFromX(posizioneCorrenteX);
                this.transizioneAttiva.setToX(posizioneCorrenteX - lunghezzaSpostamento);

                this.transizioneAttiva.setOnFinished((ActionEvent event) -> {
                    this.transizioneAttiva = null;
                });
                this.transizioneAttiva.play();
            }
        }
    }
}
