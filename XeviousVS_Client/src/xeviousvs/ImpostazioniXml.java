package xeviousvs;

import java.io.Serializable;

public class ImpostazioniXml implements Serializable {

    public AssociazioniTasti associazioniTasti;
    public PortaAscoltoClient portaAscoltoClient;
    public IndirizzoServer indirizzoDatabase;
    public IndirizzoServer indirizzoServerLog;

    public ImpostazioniXml(AssociazioniTasti associazioniTasti, IndirizzoServer indirizzoDatabase, IndirizzoServer indirizzoServerLog, PortaAscoltoClient portaAscoltoClient) {
        this.associazioniTasti = associazioniTasti;
        this.indirizzoDatabase = indirizzoDatabase;
        this.indirizzoServerLog = indirizzoServerLog;
        this.portaAscoltoClient = portaAscoltoClient;
    }
}
