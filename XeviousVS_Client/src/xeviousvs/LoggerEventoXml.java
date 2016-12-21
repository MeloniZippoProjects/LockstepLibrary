package xeviousvs;

import com.thoughtworks.xstream.XStream;
import java.io.*;
import java.net.*;
import javax.xml.XMLConstants;
import javax.xml.parsers.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.*;
import org.w3c.dom.Document;
import org.xml.sax.*;

public class LoggerEventoXml {

    private static java.net.InetSocketAddress indirizzoServerLog;

    private static final String percorsoFileXsd = "entrataLog.xsd";

    public static final String descrizioneEventoAvvio = "ApplicazioneAvviata";
    public static final String descrizioneEventoChiusura = "Applicazione chiusa";

    public static final String descrizioneEventoUsername = "Username inserito";
    public static final String descrizioneEventoBottone = "Pulsante - premuto";

    public static final String descrizioneEventoAvvioPartita = "Partita avviata";
    public static final String descrizioneEventoTerminazionePartita = "Partita terminata";

    public static final String descrizioneEventoPausaPartita = "Partita messa in pausa";
    public static final String descrizioneEventoRipresaPartita = "Partita ripresa";

    public static final String descrizioneEventoInterruzionePartita = "Partita interrotta";
    public static final String descrizioneEventoRecuperoPartita = "Partita recuperata";

    public static final String descrizioneEventoAssociazioniTasti = "Caricate associazioni tasti personalizzate. Destra: - Sinistra: - Fuoco: - Pausa: -";
    public static final String segnapostoDescrizione = "-";

    public static void impostaIndirizzoServerLog(String indirizzo, int porta)
    {
        LoggerEventoXml.indirizzoServerLog = new InetSocketAddress(indirizzo, porta);
    }
    
    public static void registraEvento(String descrizioneEvento) {
        LogEventoXml log = new LogEventoXml(descrizioneEvento);
        String logSerializzato = serializzaLogEvento(log);
        if (validaLogEventoSerializzato(logSerializzato)) {
            inviaLogEventoSerializzato(logSerializzato);
        }
    }

    private static String serializzaLogEvento(LogEventoXml logEvento) {
        XStream xs = new XStream();
        return xs.toXML(logEvento);
    }

    private static boolean validaLogEventoSerializzato(String logEventoSerializzato) {
        try {
            DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

            Document d = db.parse(new InputSource(new ByteArrayInputStream(logEventoSerializzato.getBytes("utf-8"))));
            Schema s = sf.newSchema(new StreamSource(new File(percorsoFileXsd)));
            s.newValidator().validate(new DOMSource(d));
        } catch (ParserConfigurationException | SAXException | IOException e) {
            if (e instanceof SAXException) {
                System.out.println("Errore di validazione:" + e.getMessage());
                return false;
            } else {
                e.printStackTrace();
                return false;
            }
        }

        return true;
    }

    private static void inviaLogEventoSerializzato(String logEventoSerializzato) {
        try (
                Socket s = new Socket(indirizzoServerLog.getAddress(), indirizzoServerLog.getPort());
                DataOutputStream dout = new DataOutputStream(s.getOutputStream());) {
            dout.writeUTF(logEventoSerializzato);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
