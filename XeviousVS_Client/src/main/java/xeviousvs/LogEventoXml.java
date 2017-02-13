package xeviousvs;

import java.io.Serializable;
import java.net.*;
import java.sql.Timestamp;
import java.util.*;

public class LogEventoXml implements Serializable {

    private String indirizzoIP;
    private final String timestampEvento;
    private final String descrizioneEvento;

    public LogEventoXml(String descrizioneEvento) {
        try {
            this.indirizzoIP = InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            e.printStackTrace();
            this.indirizzoIP = "sconosciuto";
        }

        Date d = new Date();
        Timestamp t = new Timestamp(d.getTime());
        this.timestampEvento = t.toString();

        this.descrizioneEvento = descrizioneEvento;
    }

}
