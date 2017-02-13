package xeviousvs;

import javafx.geometry.Pos;
import javafx.scene.image.*;
import javafx.scene.layout.HBox;

public class PannelloViteUtente {

    private final ImageView[] vistaVite;
    private final HBox vistaPannelloViteUtente;
    private final int massimoVite;

    private static final String percorsoCuorePieno = "file:./../img/cuorePieno.png";
    private static final String percorsoCuoreVuoto = "file:./../img/cuoreVuoto.png";

    public PannelloViteUtente(HBox vistaPannelloViteUtente, int massimoVite) {
        this.vistaPannelloViteUtente = vistaPannelloViteUtente;
        this.massimoVite = massimoVite;

        this.vistaVite = new ImageView[massimoVite];
        this.vistaPannelloViteUtente.setAlignment(Pos.CENTER);
        for (int i = 0; i < massimoVite; i++) {
            ImageView imView = new ImageView();
            imView.setFitHeight(this.vistaPannelloViteUtente.getPrefHeight() * 0.9);
            imView.setFitWidth(this.vistaPannelloViteUtente.getPrefHeight() * 0.9);
            this.vistaVite[i] = imView;
            this.vistaPannelloViteUtente.getChildren().add(imView);
        }

        this.aggiornaVistaVite(massimoVite);
    }

    public void aggiornaVistaVite(int viteUtente) {
        Image cuorePieno = new Image(percorsoCuorePieno);
        Image cuoreVuoto = new Image(percorsoCuoreVuoto);

        for (int i = 0; i < this.massimoVite; i++) {
            if (i < viteUtente) {
                vistaVite[i].setImage(cuorePieno);
            } else {
                vistaVite[i].setImage(cuoreVuoto);
            }
        }
    }
}
