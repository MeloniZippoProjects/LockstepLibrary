package xeviousvs;

import java.io.Serializable;

public class ImpostazioniXml implements Serializable {

    public AssociazioniTasti associazioniTasti;
    public PortaAscoltoClient portaAscoltoClient;
    public IndirizzoServer indirizzoDatabase;
    public IndirizzoServer indirizzoServerLog;
    public int framerate;
    public int tickrate;
    public int fillsize;
    public int timeout;

    public ImpostazioniXml(AssociazioniTasti associazioniTasti, IndirizzoServer indirizzoDatabase, IndirizzoServer indirizzoServerLog, PortaAscoltoClient portaAscoltoClient, int framerate, int tickrate, int fillsize, int timeout) {
        this.associazioniTasti = associazioniTasti;
        this.indirizzoDatabase = indirizzoDatabase;
        this.indirizzoServerLog = indirizzoServerLog;
        this.portaAscoltoClient = portaAscoltoClient;
        this.framerate = framerate;
        this.tickrate = tickrate;
        this.fillsize = fillsize;
        this.timeout = timeout;
    }
}
