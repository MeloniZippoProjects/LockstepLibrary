package xeviousvs;

import java.io.Serializable;
import javafx.scene.input.KeyCode;

public class AssociazioniTasti implements Serializable {

    public KeyCode tastoDestra;
    public KeyCode tastoSinistra;
    public KeyCode tastoFuoco;
    public KeyCode tastoPausa;

    public AssociazioniTasti(KeyCode tastoDestra, KeyCode tastoSinistra, KeyCode tastoFuoco, KeyCode tastoPausa) {
        this.tastoDestra = tastoDestra;
        this.tastoSinistra = tastoSinistra;
        this.tastoFuoco = tastoFuoco;
        this.tastoPausa = tastoPausa;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj.getClass() == AssociazioniTasti.class) {
            AssociazioniTasti ass = (AssociazioniTasti) obj;
            if (ass.tastoDestra == this.tastoDestra
                    && ass.tastoSinistra == this.tastoSinistra
                    && ass.tastoFuoco == this.tastoFuoco
                    && ass.tastoPausa == this.tastoPausa) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }
}
