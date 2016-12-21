package xeviousvs;

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

public class XeviousVS extends Application {

    private TextField campoInputUsername;
    private Button bottoneGiocaAnnulla;
    private Label vistaStatisticheUtente;
    private Label vistaMessaggio;
    private String usernameGiocatore;
    private VistaGioco vistaGioco;
    private ModelloGioco modelloGioco;
    private RicercatoreAvversario ricercatorePartita;
    private AssociazioniTasti associazioneTasti;

    private TrasmettitoreComandi trasmettitoreComandi;
    private RicevitoreComandi ricevitoreComandi;

    private static final int paddingInterfaccia = 15;
    private static final int altezzaContenitoreGioco = 500;
    private static final int larghezzaContenitoreGioco = 800;
    private static final int altezzaPulsante = 80;
    private static final int larghezzaPulsante = 100;
    private static final int paddingPulsante = 10;

    private static final String testoTitolo = "XeviousVS";
    private static final String testoEtichettaUsername = "Username";
    private static final String testoVistaStatistiche = "Vittorie: -\nSconfitte: -\nPercentuale vittorie= - %";
    private static final String testoBottoneGioca = "Gioca!";
    private static final String testoBottoneAnnulla = "Annulla attesa";

    private static final String segnapostoSostituizioniStatistiche = "-";
    public static final String segnapostoSostituizioniMessaggi = "-?-";

    public static final String messaggioRicercaPartita = "Ricerca nuova partita in corso...";
    public static final String messaggioRecuperoPartita = "Recupero della partita interrotta in corso...";
    public static final String messaggioAttesaInterrotta = "Attesa interrotta";
    public static final String messaggioAvversarioTrovato = "Avversario trovato!";
    public static final String messaggioPartitaPronta = "Partita con -?- pronta, premi FUOCO per cominciare";
    public static final String messaggioAttesaAvversario = "-?- non è ancora pronto";
    public static final String messaggioPartitaInCorso = "Partita in corso";
    public static final String messaggioPartitaInPausaGiocatore = "Partita in pausa. Premi FUOCO per riprendere";
    public static final String messaggioPartitaInPausaAvversario = "-?- ha messo in pausa la partita";
    public static final String messaggioVittoria = "Hai vinto contro -?-! Premi Gioca! per cercare un altro avversario";
    public static final String messaggioSconfitta = "-?- ha vinto... Premi Gioca! per cercare un altro avversario";

    public void start(Stage primaryStage) {
        VBox root = costruisciInterfaccia();

        this.modelloGioco = new ModelloGioco(this);
        this.modelloGioco.assegnaVistaGioco(vistaGioco);
        this.vistaGioco.assegnaModelloGioco(modelloGioco);

        caricaImpostazioni();

        this.impostaBottoneGioca(false);

        OperazioniCacheLocale.ripristinaCache(this, modelloGioco, vistaGioco);

        this.campoInputUsername.setOnAction((ActionEvent ev) -> {
            this.registraUtente(this.campoInputUsername.getText());
            LoggerEventoXml.registraEvento(LoggerEventoXml.descrizioneEventoUsername);
        });

        Scene scene = new Scene(root);
        scene.setOnKeyPressed((KeyEvent ev) -> {
            this.riceviInputGiocatore(ev.getCode());
        });

        primaryStage.setOnCloseRequest((WindowEvent ev) -> {
            this.ricercatorePartita.interrompiRicerca();
            if (this.ricevitoreComandi != null) {
                this.ricevitoreComandi.interrupt();
            }
            this.ricercatorePartita.chiudiConnessioneAvversario(false);

            OperazioniCacheLocale.salvaCache(usernameGiocatore, modelloGioco, vistaGioco);
            LoggerEventoXml.registraEvento(LoggerEventoXml.descrizioneEventoChiusura);
            Platform.exit();
        });

        primaryStage.setTitle(XeviousVS.testoTitolo);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void caricaImpostazioni() {
        ImpostazioniXml impostazioniXml = LettoreImpostazioniXml.leggiImpostazioni();
        this.associazioneTasti = impostazioniXml.associazioniTasti;
        this.ricercatorePartita = new RicercatoreAvversario(impostazioniXml.portaAscoltoClient.porta, this, this.modelloGioco);
        LoggerEventoXml.impostaIndirizzoServerLog(impostazioniXml.indirizzoServerLog.indirizzoIP, impostazioniXml.indirizzoServerLog.porta);
        OperazioniDatabase.impostaIndirizzoDatabase(impostazioniXml.indirizzoDatabase.indirizzoIP, impostazioniXml.indirizzoDatabase.porta);

        LoggerEventoXml.registraEvento(LoggerEventoXml.descrizioneEventoAvvio);

        if (!impostazioniXml.associazioniTasti.equals(LettoreImpostazioniXml.impostazioniDefault.associazioniTasti)) {      //1
            String descrizioneEvento = LoggerEventoXml.descrizioneEventoAssociazioniTasti.replaceFirst(LoggerEventoXml.segnapostoDescrizione, impostazioniXml.associazioniTasti.tastoDestra.getName());
            descrizioneEvento = descrizioneEvento.replaceFirst(LoggerEventoXml.segnapostoDescrizione, impostazioniXml.associazioniTasti.tastoSinistra.getName());
            descrizioneEvento = descrizioneEvento.replaceFirst(LoggerEventoXml.segnapostoDescrizione, impostazioniXml.associazioniTasti.tastoFuoco.getName());
            descrizioneEvento = descrizioneEvento.replaceFirst(LoggerEventoXml.segnapostoDescrizione, impostazioniXml.associazioniTasti.tastoPausa.getName());
            LoggerEventoXml.registraEvento(descrizioneEvento);
        }
    }

    private VBox costruisciInterfaccia() {
        VBox root = new VBox();
        root.setPadding(new Insets(paddingInterfaccia));

        Label etichettaUsername = new Label(XeviousVS.testoEtichettaUsername);
        this.campoInputUsername = new TextField();
        HBox usernameLayout = new HBox(etichettaUsername, this.campoInputUsername);
        usernameLayout.setAlignment(Pos.CENTER);

        this.vistaStatisticheUtente = new Label(XeviousVS.testoVistaStatistiche);

        this.bottoneGiocaAnnulla = new Button();
        this.bottoneGiocaAnnulla.setPrefSize(XeviousVS.larghezzaPulsante, XeviousVS.altezzaPulsante);
        this.bottoneGiocaAnnulla.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        this.bottoneGiocaAnnulla.setPadding(new Insets(XeviousVS.paddingPulsante));

        this.vistaMessaggio = new Label();

        VBox contenitoreGioco = new VBox();
        contenitoreGioco.setPrefSize(XeviousVS.larghezzaContenitoreGioco, XeviousVS.altezzaContenitoreGioco);
        this.vistaGioco = new VistaGioco(contenitoreGioco);

        root.getChildren().addAll(usernameLayout, this.vistaStatisticheUtente, this.bottoneGiocaAnnulla, this.vistaMessaggio, contenitoreGioco);
        root.setAlignment(Pos.CENTER);

        return root;
    }

    private void registraUtente(String usernameGiocatore) {
        this.usernameGiocatore = usernameGiocatore;
        StatisticheUtente statistiche = OperazioniDatabase.leggiStatisticheUtente(usernameGiocatore);
        if (statistiche == null) {
            OperazioniDatabase.registraNuovoUtente(usernameGiocatore);
            statistiche = OperazioniDatabase.leggiStatisticheUtente(usernameGiocatore);
        }
        this.aggiornaVistaStatistiche(statistiche);

        this.bottoneGiocaAnnulla.setDisable(false);
    }

    private void aggiornaVistaStatistiche(StatisticheUtente statisticheUtente) {
        String messaggioStatistiche = XeviousVS.testoVistaStatistiche.replaceFirst(XeviousVS.segnapostoSostituizioniStatistiche, Integer.toString(statisticheUtente.numeroVittorie));
        messaggioStatistiche = messaggioStatistiche.replaceFirst(XeviousVS.segnapostoSostituizioniStatistiche, Integer.toString(statisticheUtente.numeroSconfitte));

        DecimalFormat df = new DecimalFormat("#0.00");
        messaggioStatistiche = messaggioStatistiche.replaceFirst(XeviousVS.segnapostoSostituizioniStatistiche, df.format(statisticheUtente.percentualeVittorie));
        this.vistaStatisticheUtente.setText(messaggioStatistiche);
    }

    private void riceviInputGiocatore(KeyCode tasto) {
        if (this.associazioneTasti != null && this.trasmettitoreComandi != null) {
            if (tasto == this.associazioneTasti.tastoDestra) {
                this.trasmettitoreComandi.inoltraComando(Comando.Destra);
                this.modelloGioco.eseguiComandoGiocatore(Comando.Destra);
            }
            if (tasto == this.associazioneTasti.tastoSinistra) {
                this.trasmettitoreComandi.inoltraComando(Comando.Sinistra);
                this.modelloGioco.eseguiComandoGiocatore(Comando.Sinistra);
            }
            if (tasto == this.associazioneTasti.tastoFuoco) {
                this.trasmettitoreComandi.inoltraComando(Comando.Fuoco);
                this.modelloGioco.eseguiComandoGiocatore(Comando.Fuoco);
            }
            if (tasto == this.associazioneTasti.tastoPausa) {
                this.trasmettitoreComandi.inoltraComando(Comando.Pausa);
                this.modelloGioco.eseguiComandoGiocatore(Comando.Pausa);
            }
        }
    }

    public void impostaBottoneGioca(boolean abilitato) {
        this.bottoneGiocaAnnulla.setText(XeviousVS.testoBottoneGioca);
        this.bottoneGiocaAnnulla.setDisable(!abilitato);
        this.bottoneGiocaAnnulla.setOnAction((ActionEvent ev) -> {
            LoggerEventoXml.registraEvento(LoggerEventoXml.descrizioneEventoBottone.replace(LoggerEventoXml.segnapostoDescrizione, XeviousVS.testoBottoneGioca));
            this.bottoneGiocaAnnulla.setDisable(true);
            this.campoInputUsername.setDisable(true);
            this.vistaGioco.ottieniVistaAreaGioco().requestFocus();
            this.impostaMessaggio(XeviousVS.messaggioRicercaPartita);
            this.ricercatorePartita.avviaRicercaNuovaPartita();
        });
    }

    public void impostaBottoneAnnulla(boolean abilitato) {
        this.bottoneGiocaAnnulla.setText(XeviousVS.testoBottoneAnnulla);
        this.bottoneGiocaAnnulla.setDisable(!abilitato);
        this.bottoneGiocaAnnulla.setOnAction((ActionEvent ev) -> {
            LoggerEventoXml.registraEvento(LoggerEventoXml.descrizioneEventoBottone.replace(LoggerEventoXml.segnapostoDescrizione, XeviousVS.testoBottoneAnnulla));
            this.campoInputUsername.setDisable(false);
            this.impostaBottoneGioca(true);
            this.ricercatorePartita.interrompiRicerca();
        });
    }

    public void impostaMessaggio(String messaggio) {
        this.vistaMessaggio.setText(messaggio);
    }

    public void impostaUsernameRecuperato(String usernameGiocatore) {
        this.campoInputUsername.setText(usernameGiocatore);
        this.registraUtente(usernameGiocatore);
    }

    public void impostaRecuperoPartita() {
        LoggerEventoXml.registraEvento(LoggerEventoXml.descrizioneEventoInterruzionePartita);
        
        this.campoInputUsername.setDisable(true);
        this.impostaBottoneAnnulla(false);
        this.impostaMessaggio(XeviousVS.messaggioRecuperoPartita);
        this.vistaGioco.ottieniVistaAreaGioco().requestFocus();
        this.ricercatorePartita.avviaRecuperoPartita();
    }

    public void impostaTerminazionePartita(boolean vittoria) {
        this.ricercatorePartita.chiudiConnessioneAvversario(true);
        OperazioniDatabase.aggiornaStatisticheUtente(usernameGiocatore, vittoria);
        this.aggiornaVistaStatistiche(OperazioniDatabase.leggiStatisticheUtente(usernameGiocatore));
        this.impostaBottoneGioca(true);
        this.campoInputUsername.setDisable(false);
        LoggerEventoXml.registraEvento(LoggerEventoXml.descrizioneEventoTerminazionePartita);
    }

    public String ottieniUsernameGiocatore() {
        return this.usernameGiocatore;
    }

    public void impostaConnessionePartita(RicevitoreComandi ricevitore, TrasmettitoreComandi trasmettitore) {
        this.ricevitoreComandi = ricevitore;
        this.trasmettitoreComandi = trasmettitore;
    }

    public static void main(String[] args) {
        launch(args);
    }
}

/*
1: è necessario controllare la presenza di associazioni di tasti default dopo il caricamento e l'uso delle impostazioni perché altrimenti 
    non si troverebbe configurata la classe LoggerEventoXml per inviare il messaggio di log
*/
