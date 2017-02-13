package xeviousvs;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Bounds;

class RilevatoreSpostamento implements ChangeListener<Bounds> {

    Proiettile proiettileOsservato;

    public RilevatoreSpostamento(Proiettile proiettileOsservato) {
        super();
        this.proiettileOsservato = proiettileOsservato;
    }

    @Override
    public void changed(ObservableValue<? extends Bounds> observable, Bounds oldValue, Bounds newValue) {
        this.proiettileOsservato.controllaImpatto();
    }
}
