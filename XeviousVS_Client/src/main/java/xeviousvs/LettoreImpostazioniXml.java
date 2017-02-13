package xeviousvs;

import com.thoughtworks.xstream.XStream;
import java.io.ByteArrayInputStream;
import java.nio.file.*;
import javafx.scene.input.KeyCode;
import javax.xml.XMLConstants;
import javax.xml.parsers.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.*;
import org.w3c.dom.Document;
import org.xml.sax.*;

public class LettoreImpostazioniXml {

    private static final String percorsoFileXml = "impostazioni.xml";
    private static final String percorsoFileXsd = "impostazioni.xsd";
    public static final ImpostazioniXml impostazioniDefault = new ImpostazioniXml(
            new AssociazioniTasti(KeyCode.D, KeyCode.A, KeyCode.S, KeyCode.Q),
            new IndirizzoServer("localhost", 3306),
            new IndirizzoServer("localhost", 9200),
            new PortaAscoltoClient(9000, 9000, 9100));

    public static ImpostazioniXml leggiImpostazioni() {
        String x = "";

        try {
            x = new String(Files.readAllBytes(Paths.get(LettoreImpostazioniXml.percorsoFileXml)));
        } catch (Exception e) {
        }

        if (validaImpostazioniXml(x)) {
            return deserializzaImpostazioniXml(x);
        } else {
            scriviImpostazioniDefault();
            return LettoreImpostazioniXml.impostazioniDefault;
        }
    }

    private static boolean validaImpostazioniXml(String impostazioniXmlSerializzate) {
        try {
            DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

            Document d = db.parse(new InputSource(new ByteArrayInputStream(impostazioniXmlSerializzate.getBytes("utf-8"))));
            Schema s = sf.newSchema(new StreamSource(Files.newInputStream(Paths.get(LettoreImpostazioniXml.percorsoFileXsd))));
            s.newValidator().validate(new DOMSource(d));
        } catch (Exception e) {
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

    private static ImpostazioniXml deserializzaImpostazioniXml(String impostazioniXmlSerializzate) {
        XStream xs = new XStream();
        xs.setMode(XStream.NO_REFERENCES);
        xs.useAttributeFor(PortaAscoltoClient.class, "portaMinima");
        xs.useAttributeFor(PortaAscoltoClient.class, "portaMassima");
        return (ImpostazioniXml) xs.fromXML(impostazioniXmlSerializzate);
    }

    private static void scriviImpostazioniDefault() {
        XStream xs = new XStream();
        xs.setMode(XStream.NO_REFERENCES);
        xs.useAttributeFor(PortaAscoltoClient.class, "portaMinima");
        xs.useAttributeFor(PortaAscoltoClient.class, "portaMassima");
        String x = xs.toXML(impostazioniDefault);

        try {
            Files.write(Paths.get(LettoreImpostazioniXml.percorsoFileXml), x.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
