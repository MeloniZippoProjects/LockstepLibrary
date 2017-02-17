package xeviousvs.client;

import java.net.InetSocketAddress;
import java.text.DecimalFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.*;
import javafx.event.ActionEvent;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
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

    private XeviousLockstepClient lockstepClient;
    private Thread lockstepClientThread;

    private static final int paddingInterfaccia = 15;
    private static final int altezzaContenitoreGioco = 500;
    private static final int larghezzaContenitoreGioco = 800;
    private static final int altezzaPulsante = 80;
    private static final int larghezzaPulsante = 100;
    private static final int paddingPulsante = 10;

    private int framerate;
    private int tickrate;
    private int fillsize;
    private int timeout;

    private static final String testoTitolo = "XeviousVS";
    private static final String testoEtichettaUsername = "Username";
    private static final String testoVistaStatistiche = "Vittorie: -\nSconfitte: -\nPercentuale vittorie= - %";
    private static final String testoBottoneGioca = "Gioca!";
    private static final String testoBottoneAnnulla = "Annulla attesa";

    private static final String segnapostoSostituizioniStatistiche = "-";
    public static final String segnapostoSostituizioniMessaggi = "-?-";

    public static final String messaggioRicercaPartita = "Ricerca nuova partita in corso...";
    public static final String messaggioServerNonTrovato = "Nessun server disponibile trovato";
    public static final String messaggioServerTrovato = "Server trovato, in attesa inizio partita...";
    public static final String messaggioPartitaPronta = "Partita con -?- pronta, premi FUOCO per cominciare";
    public static final String messaggioAttesaAvversario = "-?- non e' ancora pronto";
    public static final String messaggioPartitaInCorso = "Partita in corso";
    public static final String messaggioPartitaInPausaGiocatore = "Partita in pausa. Premi FUOCO per riprendere";
    public static final String messaggioPartitaInPausaAvversario = "-?- ha messo in pausa la partita";
    public static final String messaggioVittoria = "Hai vinto contro -?-! Premi Gioca! per cercare un altro avversario";
    public static final String messaggioSconfitta = "-?- ha vinto... Premi Gioca! per cercare un altro avversario";

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
            LoggerEventoXml.registraEvento(LoggerEventoXml.descrizioneEventoUsername);
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
                lockstepClientThread.interrupt();
                try
                {
                    lockstepClientThread.join(1500);
                }
                catch (InterruptedException ex)
                {
                    ex.printStackTrace();
                }
            }

            LoggerEventoXml.registraEvento(LoggerEventoXml.descrizioneEventoChiusura);
            Platform.exit();
        });

        primaryStage.setTitle(XeviousVS_Client.testoTitolo);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void caricaImpostazioni()
    {
        ImpostazioniXml impostazioniXml = LettoreImpostazioniXml.leggiImpostazioni();
        this.associazioneTasti = impostazioniXml.associazioniTasti;
        //this.ricercatorePartita = new RicercatoreAvversario(impostazioniXml.portaAscoltoClient.porta, this, this.modelloGioco);
        LoggerEventoXml.impostaIndirizzoServerLog(impostazioniXml.indirizzoServerLog.indirizzoIP, impostazioniXml.indirizzoServerLog.porta);
        OperazioniDatabaseClient.impostaIndirizzoDatabase(impostazioniXml.indirizzoDatabase.indirizzoIP, impostazioniXml.indirizzoDatabase.porta);

        tickrate = impostazioniXml.tickrate;
        fillsize = impostazioniXml.fillsize;
        framerate = impostazioniXml.framerate;

        LoggerEventoXml.registraEvento(LoggerEventoXml.descrizioneEventoAvvio);

        if (!impostazioniXml.associazioniTasti.equals(LettoreImpostazioniXml.impostazioniDefault.associazioniTasti))
        {
            String descrizioneEvento = LoggerEventoXml.descrizioneEventoAssociazioniTasti.replaceFirst(LoggerEventoXml.segnapostoDescrizione, impostazioniXml.associazioniTasti.tastoDestra.getName());
            descrizioneEvento = descrizioneEvento.replaceFirst(LoggerEventoXml.segnapostoDescrizione, impostazioniXml.associazioniTasti.tastoSinistra.getName());
            descrizioneEvento = descrizioneEvento.replaceFirst(LoggerEventoXml.segnapostoDescrizione, impostazioniXml.associazioniTasti.tastoFuoco.getName());
            descrizioneEvento = descrizioneEvento.replaceFirst(LoggerEventoXml.segnapostoDescrizione, impostazioniXml.associazioniTasti.tastoPausa.getName());
            LoggerEventoXml.registraEvento(descrizioneEvento);
        }
    }

    private VBox costruisciInterfaccia()
    {
        VBox root = new VBox();
        root.setPadding(new Insets(paddingInterfaccia));

        Label etichettaUsername = new Label(XeviousVS_Client.testoEtichettaUsername);
        this.campoInputUsername = new TextField();
        HBox usernameLayout = new HBox(etichettaUsername, this.campoInputUsername);
        usernameLayout.setAlignment(Pos.CENTER);

        this.vistaStatisticheUtente = new Label(XeviousVS_Client.testoVistaStatistiche);

        this.bottoneGiocaAnnulla = new Button();
        this.bottoneGiocaAnnulla.setPrefSize(XeviousVS_Client.larghezzaPulsante, XeviousVS_Client.altezzaPulsante);
        this.bottoneGiocaAnnulla.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        this.bottoneGiocaAnnulla.setPadding(new Insets(XeviousVS_Client.paddingPulsante));

        this.vistaMessaggio = new Label();

        VBox contenitoreGioco = new VBox();
        contenitoreGioco.setPrefSize(XeviousVS_Client.larghezzaContenitoreGioco, XeviousVS_Client.altezzaContenitoreGioco);
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
        String messaggioStatistiche = XeviousVS_Client.testoVistaStatistiche.replaceFirst(XeviousVS_Client.segnapostoSostituizioniStatistiche, Integer.toString(statisticheUtente.numeroVittorie));
        messaggioStatistiche = messaggioStatistiche.replaceFirst(XeviousVS_Client.segnapostoSostituizioniStatistiche, Integer.toString(statisticheUtente.numeroSconfitte));

        DecimalFormat df = new DecimalFormat("#0.00");
        messaggioStatistiche = messaggioStatistiche.replaceFirst(XeviousVS_Client.segnapostoSostituizioniStatistiche, df.format(statisticheUtente.percentualeVittorie));
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
        this.bottoneGiocaAnnulla.setText(XeviousVS_Client.testoBottoneGioca);
        this.bottoneGiocaAnnulla.setDisable(!abilitato);
        this.bottoneGiocaAnnulla.setOnAction((ActionEvent ev) ->
        {
            LoggerEventoXml.registraEvento(LoggerEventoXml.descrizioneEventoBottone.replace(LoggerEventoXml.segnapostoDescrizione, XeviousVS_Client.testoBottoneGioca));
            this.bottoneGiocaAnnulla.setDisable(true);
            this.campoInputUsername.setDisable(true);
            this.vistaGioco.ottieniVistaAreaGioco().requestFocus();
            this.impostaMessaggio(XeviousVS_Client.messaggioRicercaPartita);

            ServerDisponibile server = OperazioniDatabaseClient.ottieniServerDisponibile();
            if (server == null)
            {
                this.impostaMessaggio(messaggioServerNonTrovato);
                this.impostaBottoneGioca(true);            
            }
            else
            {
                modelloGioco.resetPartita();
                this.impostaMessaggio(messaggioServerTrovato);
                InetSocketAddress serverAddress = new InetSocketAddress(server.indirizzoAscolto, server.portaAscolto);
                lockstepClient = new XeviousLockstepClient(serverAddress, framerate, tickrate, timeout, fillsize, usernameGiocatore, this, modelloGioco, comandoCorrente);
                lockstepClientThread = new Thread(lockstepClient);
                lockstepClientThread.start();
            }
        });
    }

    public void impostaBottoneAnnulla(boolean abilitato)
    {
        this.bottoneGiocaAnnulla.setText(XeviousVS_Client.testoBottoneAnnulla);
        this.bottoneGiocaAnnulla.setDisable(!abilitato);
        this.bottoneGiocaAnnulla.setOnAction((ActionEvent ev) ->
        {
            LoggerEventoXml.registraEvento(LoggerEventoXml.descrizioneEventoBottone.replace(LoggerEventoXml.segnapostoDescrizione, XeviousVS_Client.testoBottoneAnnulla));
            this.campoInputUsername.setDisable(false);
            this.impostaBottoneGioca(true);
            //this.ricercatorePartita.interrompiRicerca();
        });
    }

    public void impostaMessaggio(String messaggio)
    {
        this.vistaMessaggio.setText(messaggio);
    }

    public void impostaTerminazionePartita(boolean vittoria)
    {
        //this.ricercatorePartita.chiudiConnessioneAvversario(true);
        OperazioniDatabaseClient.aggiornaStatisticheUtente(usernameGiocatore, vittoria);
        this.aggiornaVistaStatistiche(OperazioniDatabaseClient.leggiStatisticheUtente(usernameGiocatore));
        this.impostaBottoneGioca(true);
        this.campoInputUsername.setDisable(false);
        LoggerEventoXml.registraEvento(LoggerEventoXml.descrizioneEventoTerminazionePartita);
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
