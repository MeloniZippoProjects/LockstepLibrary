package xeviousvs.client;

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
import xeviousvs.LogEventoXml;

public class LoggerEventoXml {

    private static java.net.InetSocketAddress indirizzoServerLog;

    private static final String XSD_PATH = "entrataLog.xsd";

    public static final String LOG_AVVIO = "ApplicazioneAvviata";
    public static final String LOG_CHIUSURA = "Applicazione chiusa";

    public static final String LOG_USERNAME_INSERITO = "Username inserito";
    public static final String LOG_BOTTONE = "Pulsante - premuto";

    public static final String LOG_SERVER_NON_VALIDO = "Handshake col server fallito";
    public static final String LOG_PARTITA_AVVIATA = "Partita avviata";
    public static final String LOG_PARTITA_CONCLUSA = "Partita terminata";
    public static final String LOG_PARTITA_DISCONNESSA = "Partita interrotta per disconnessione";    
    
    public static final String LOG_PARTITA_PAUSA = "Partita messa in pausa";
    public static final String LOG_PARTITA_RIPRESA = "Partita ripresa";

    public static final String LOG_ASSOCIAZIONI_TASTI = "Caricate associazioni tasti personalizzate. Destra: - Sinistra: - Fuoco: - Pausa: -";
    public static final String LOG_PLACEHOLDER = "-";

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
            Schema s = sf.newSchema(new StreamSource( XeviousVS_Client.class.getClassLoader().getResource(XSD_PATH).openStream()));
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
