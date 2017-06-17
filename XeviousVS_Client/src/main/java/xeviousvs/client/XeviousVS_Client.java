package xeviousvs.client;

import xeviousvs.client.gioco.ModelloGioco;
import xeviousvs.client.gioco.VistaGioco;
import java.net.InetSocketAddress;
import java.text.DecimalFormat;
import javafx.application.*;
import javafx.event.ActionEvent;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import lockstep.LockstepClient;
import xeviousvs.Comando;
import xeviousvs.Comando.EnumComando;

public class XeviousVS_Client extends Application
{

    private TextField campoInputUsername;
    private Button bottoneGiocaAnnulla;
    private Label vistaStatisticheUtente;
    private Label vistaMessaggio;
    private String usernameGiocatore;
    private VistaGioco vistaGioco;
    private ModelloGioco modelloGioco;
    private AssociazioniTasti associazioneTasti;

    private Comando comandoCorrente;

    private XeviousVSLockstepApplication lockstepApplication;
    private LockstepClient lockstepClient;

    private static final int PADDING_INTERFACCIA = 15;
    private static final int ALTEZZA_CONTENITORE_GIOCO = 500;
    private static final int LARGHEZZA_CONTENITORE_GIOCO = 800;
    private static final int ALTEZZA_PULSANTE = 80;
    private static final int LARGHEZZA_PULSANTE = 100;
    private static final int PADDING_PULSANTE = 10;

    private int framerate;
    private int tickrate;
    private int delay;
    private int timeout;

    private static final String TITOLO = "XeviousVS";
    private static final String USERNAME_LABEL = "Username";
    private static final String STAT_TESTO = "Vittorie: -\nSconfitte: -\nPercentuale vittorie= - %";
    private static final String GIOCA_TESTO = "Gioca!";
    private static final String ANNULLA_TESTO = "Annulla attesa";

    private static final String STAT_PLACEHOLDER = "-";
    public static final String MSG_PLACEHOLDER = "-?-";

    public static final String MSG_RICERCA_PARTITA = "Ricerca nuova partita in corso...";
    public static final String MSG_SERVER_NON_TROVATO = "Nessun server disponibile trovato";
    public static final String MSG_SERVER_TROVATO = "Server trovato, in attesa inizio partita...";
    public static final String MSG_HANDSHAKE_FALLITO = "Errore di comunicazione col server, partita annullata";
    public static final String MSG_PARTITA_PRONTA = "Partita con -?- pronta, premi FUOCO per cominciare";
    public static final String MSG_ATTESA_AVVERSARIO = "-?- non e' ancora pronto";
    public static final String MSG_PARTITA_IN_CORSO = "Partita in corso";
    public static final String MSG_PARTITA_IN_PAUSA_GIOCATORE = "Partita in pausa. Premi FUOCO per riprendere";
    public static final String MSG_PARTITA_IN_PAUSA_AVVERSARIO = "-?- ha messo in pausa la partita";
    public static final String MSG_DISCONNESIONE_PARTITA = "Connessione persa, partita annullata";
    public static final String MSG_VITTORIA = "Hai vinto contro -?-! Premi Gioca! per cercare un altro avversario";
    public static final String MSG_SCONFITTA = "-?- ha vinto... Premi Gioca! per cercare un altro avversario";

    @Override
    public void start(Stage primaryStage)
    {
        VBox root = costruisciInterfaccia();

        caricaImpostazioni();
        
        this.modelloGioco = new ModelloGioco(this);
        this.modelloGioco.assegnaVistaGioco(vistaGioco);
        this.vistaGioco.assegnaModelloGioco(modelloGioco, framerate);

        this.impostaBottoneGioca(false);

        this.campoInputUsername.setOnAction((ActionEvent ev) ->
        {
            this.registraUtente(this.campoInputUsername.getText());
            LoggerEventoXml.registraEvento(LoggerEventoXml.LOG_USERNAME_INSERITO);
        });

        Scene scene = new Scene(root);
        scene.setOnKeyPressed((KeyEvent ev) ->
        {
            this.impostaInputGiocatore(ev.getCode());
        });

        primaryStage.setOnCloseRequest((WindowEvent ev) ->
        {
            if (lockstepClient != null)
            {
                lockstepClient.abort();
                try
                {
                    lockstepClient.join(1500);
                }
                catch (InterruptedException ex)
                {
                    ex.printStackTrace();
                }
            }

            LoggerEventoXml.registraEvento(LoggerEventoXml.LOG_CHIUSURA);
            Platform.exit();
        });

        primaryStage.setTitle(XeviousVS_Client.TITOLO);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void caricaImpostazioni()
    {
        ImpostazioniXml impostazioniXml = LettoreImpostazioniXml.leggiImpostazioni();
        this.associazioneTasti = impostazioniXml.associazioniTasti;
        LoggerEventoXml.impostaIndirizzoServerLog(impostazioniXml.indirizzoServerLog.indirizzoIP, impostazioniXml.indirizzoServerLog.porta);
        OperazioniDatabaseClient.impostaIndirizzoDatabase(impostazioniXml.indirizzoDatabase.indirizzoIP, impostazioniXml.indirizzoDatabase.porta);

        tickrate = impostazioniXml.tickrate;
        delay = impostazioniXml.delay;
        framerate = impostazioniXml.framerate;

        LoggerEventoXml.registraEvento(LoggerEventoXml.LOG_AVVIO);

        if (!impostazioniXml.associazioniTasti.equals(LettoreImpostazioniXml.impostazioniDefault.associazioniTasti))
        {
            String descrizioneEvento = LoggerEventoXml.LOG_ASSOCIAZIONI_TASTI.replaceFirst(LoggerEventoXml.LOG_PLACEHOLDER, impostazioniXml.associazioniTasti.tastoDestra.getName());
            descrizioneEvento = descrizioneEvento.replaceFirst(LoggerEventoXml.LOG_PLACEHOLDER, impostazioniXml.associazioniTasti.tastoSinistra.getName());
            descrizioneEvento = descrizioneEvento.replaceFirst(LoggerEventoXml.LOG_PLACEHOLDER, impostazioniXml.associazioniTasti.tastoFuoco.getName());
            descrizioneEvento = descrizioneEvento.replaceFirst(LoggerEventoXml.LOG_PLACEHOLDER, impostazioniXml.associazioniTasti.tastoPausa.getName());
            LoggerEventoXml.registraEvento(descrizioneEvento);
        }
    }

    private VBox costruisciInterfaccia()
    {
        VBox root = new VBox();
        root.setPadding(new Insets(PADDING_INTERFACCIA));

        Label etichettaUsername = new Label(XeviousVS_Client.USERNAME_LABEL);
        this.campoInputUsername = new TextField();
        HBox usernameLayout = new HBox(etichettaUsername, this.campoInputUsername);
        usernameLayout.setAlignment(Pos.CENTER);

        this.vistaStatisticheUtente = new Label(XeviousVS_Client.STAT_TESTO);

        this.bottoneGiocaAnnulla = new Button();
        this.bottoneGiocaAnnulla.setPrefSize(XeviousVS_Client.LARGHEZZA_PULSANTE, XeviousVS_Client.ALTEZZA_PULSANTE);
        this.bottoneGiocaAnnulla.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        this.bottoneGiocaAnnulla.setPadding(new Insets(XeviousVS_Client.PADDING_PULSANTE));

        this.vistaMessaggio = new Label();

        VBox contenitoreGioco = new VBox();
        contenitoreGioco.setPrefSize(XeviousVS_Client.LARGHEZZA_CONTENITORE_GIOCO, XeviousVS_Client.ALTEZZA_CONTENITORE_GIOCO);
        this.vistaGioco = new VistaGioco(contenitoreGioco);

        root.getChildren().addAll(usernameLayout, this.vistaStatisticheUtente, this.bottoneGiocaAnnulla, this.vistaMessaggio, contenitoreGioco);
        root.setAlignment(Pos.CENTER);

        return root;
    }

    private void registraUtente(String usernameGiocatore)
    {
        this.usernameGiocatore = usernameGiocatore;
        comandoCorrente = new Comando(EnumComando.NOP, usernameGiocatore);
        StatisticheUtente statistiche = OperazioniDatabaseClient.leggiStatisticheUtente(usernameGiocatore);
        if (statistiche == null)
        {
            OperazioniDatabaseClient.registraNuovoUtente(usernameGiocatore);
            statistiche = OperazioniDatabaseClient.leggiStatisticheUtente(usernameGiocatore);
        }
        this.aggiornaVistaStatistiche(statistiche);

        this.bottoneGiocaAnnulla.setDisable(false);
    }

    private void aggiornaVistaStatistiche(StatisticheUtente statisticheUtente)
    {
        String messaggioStatistiche = XeviousVS_Client.STAT_TESTO.replaceFirst(XeviousVS_Client.STAT_PLACEHOLDER, Integer.toString(statisticheUtente.numeroVittorie));
        messaggioStatistiche = messaggioStatistiche.replaceFirst(XeviousVS_Client.STAT_PLACEHOLDER, Integer.toString(statisticheUtente.numeroSconfitte));

        DecimalFormat df = new DecimalFormat("#0.00");
        messaggioStatistiche = messaggioStatistiche.replaceFirst(XeviousVS_Client.STAT_PLACEHOLDER, df.format(statisticheUtente.percentualeVittorie));
        this.vistaStatisticheUtente.setText(messaggioStatistiche);
    }

    private void impostaInputGiocatore(KeyCode tasto)
    {
        if (this.associazioneTasti != null && this.lockstepClient != null)
        {
            synchronized(comandoCorrente)
            {
                if (tasto == this.associazioneTasti.tastoDestra)
                {
                    if (comandoCorrente.comando == EnumComando.NOP)
                    {
                        comandoCorrente.comando = EnumComando.Destra;
                    }
                    else if (comandoCorrente.comando == EnumComando.Fuoco)
                    {
                        comandoCorrente.comando = EnumComando.FuocoDestra;
                    }
                }
                if (tasto == this.associazioneTasti.tastoSinistra)
                {
                    if (comandoCorrente.comando == EnumComando.NOP)
                    {
                        comandoCorrente.comando = EnumComando.Sinistra;
                    }
                    else if (comandoCorrente.comando == EnumComando.Fuoco)
                    {
                        comandoCorrente.comando = EnumComando.FuocoSinistra;
                    }
                }
                if (tasto == this.associazioneTasti.tastoFuoco)
                {
                    switch (comandoCorrente.comando)
                    {
                        case NOP:
                            comandoCorrente.comando = EnumComando.Fuoco;
                            break;
                        case Destra:
                            comandoCorrente.comando = EnumComando.FuocoDestra;
                            break;
                        case Sinistra:
                            comandoCorrente.comando = EnumComando.FuocoSinistra;
                            break;
                        default:
                            break;
                    }                    
                }
                if (tasto == this.associazioneTasti.tastoPausa)
                {
                    comandoCorrente.comando = EnumComando.Pausa;
                }
            }
        }
    }

    public void impostaBottoneGioca(boolean abilitato)
    {
        this.bottoneGiocaAnnulla.setText(XeviousVS_Client.GIOCA_TESTO);
        this.bottoneGiocaAnnulla.setDisable(!abilitato);
        this.bottoneGiocaAnnulla.setOnAction((ActionEvent ev) ->
        {
            LoggerEventoXml.registraEvento(LoggerEventoXml.LOG_BOTTONE.replace(LoggerEventoXml.LOG_PLACEHOLDER, XeviousVS_Client.GIOCA_TESTO));
            this.bottoneGiocaAnnulla.setDisable(true);
            this.campoInputUsername.setDisable(true);
            this.vistaGioco.ottieniVistaAreaGioco().requestFocus();
            this.impostaMessaggio(XeviousVS_Client.MSG_RICERCA_PARTITA);

            ServerDisponibile server = OperazioniDatabaseClient.ottieniServerDisponibile();
            if (server == null)
            {
                this.impostaMessaggio(MSG_SERVER_NON_TROVATO);
                this.impostaBottoneGioca(true);            
            }
            else
            {
                modelloGioco.resetPartita();
                this.impostaMessaggio(MSG_SERVER_TROVATO);
                InetSocketAddress serverAddress = new InetSocketAddress(server.indirizzoAscolto, server.portaAscolto);
                lockstepApplication = new XeviousVSLockstepApplication(framerate, delay, usernameGiocatore, this, modelloGioco, comandoCorrente);
                lockstepClient = new LockstepClient(serverAddress, framerate, tickrate, timeout, lockstepApplication);
                lockstepClient.start();
            }
        });
    }

    public void impostaBottoneAnnulla(boolean abilitato)
    {
        this.bottoneGiocaAnnulla.setText(XeviousVS_Client.ANNULLA_TESTO);
        this.bottoneGiocaAnnulla.setDisable(!abilitato);
        this.bottoneGiocaAnnulla.setOnAction((ActionEvent ev) ->
        {
            LoggerEventoXml.registraEvento(LoggerEventoXml.LOG_BOTTONE.replace(LoggerEventoXml.LOG_PLACEHOLDER, XeviousVS_Client.ANNULLA_TESTO));
            this.campoInputUsername.setDisable(false);
            this.impostaBottoneGioca(true);
        });
    }

    public void impostaMessaggio(String messaggio)
    {
        this.vistaMessaggio.setText(messaggio);
    }

    public void impostaDisconnessionePartita()
    {
        lockstepClient.abort();
        impostaMessaggio(MSG_DISCONNESIONE_PARTITA);
        this.impostaBottoneGioca(true);
        this.campoInputUsername.setDisable(false);
        LoggerEventoXml.registraEvento(LoggerEventoXml.LOG_PARTITA_DISCONNESSA);
    }
    
    public void impostaHanshakeFallito()
    {
        impostaMessaggio(MSG_HANDSHAKE_FALLITO);
        modelloGioco.interrompiPartita();
        this.impostaBottoneGioca(true);
        this.campoInputUsername.setDisable(false);
        LoggerEventoXml.registraEvento(LoggerEventoXml.LOG_SERVER_NON_VALIDO);

    }
    
    public void impostaTerminazionePartita(boolean vittoria)
    {
        lockstepClient.abort();
        OperazioniDatabaseClient.aggiornaStatisticheUtente(usernameGiocatore, vittoria);
        this.aggiornaVistaStatistiche(OperazioniDatabaseClient.leggiStatisticheUtente(usernameGiocatore));
        this.impostaBottoneGioca(true);
        this.campoInputUsername.setDisable(false);
        LoggerEventoXml.registraEvento(LoggerEventoXml.LOG_PARTITA_CONCLUSA);
    }

    public String ottieniUsernameGiocatore()
    {
        return this.usernameGiocatore;
    }

    public static void main(String[] args)
    {
        launch(args);
    }
}
