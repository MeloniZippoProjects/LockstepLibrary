package xeviousvs.client;

import java.io.Serializable;

public class ImpostazioniXml implements Serializable {

    public AssociazioniTasti associazioniTasti;
    public IndirizzoServer indirizzoDatabase;
    public IndirizzoServer indirizzoServerLog;
    public int framerate;
    public int tickrate;
    public int delay;
    public int fillTimeout;

    public ImpostazioniXml(AssociazioniTasti associazioniTasti, IndirizzoServer indirizzoDatabase, IndirizzoServer indirizzoServerLog, int framerate, int tickrate, int delay, int timeout) {
        this.associazioniTasti = associazioniTasti;
        this.indirizzoDatabase = indirizzoDatabase;
        this.indirizzoServerLog = indirizzoServerLog;
        this.framerate = framerate;
        this.tickrate = tickrate;
        this.delay = delay;
        this.fillTimeout = timeout;
    }
}
