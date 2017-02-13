package xeviousvs;

import java.sql.*;

public class OperazioniDatabase {

    private static String indirizzoDatabase;
    private static int porta;

    private static final String tipoDatabase = "mysql";
    private static final String nomeSchema = "xeviousvs";

    private static final String queryLeggiStatisticheUtente = "SELECT partite_vinte, partite_perse FROM statistiche_utenti WHERE username = ?";
    private static final String queryRegistraNuovoUtente = "INSERT INTO statistiche_utenti VALUES (?, 0, 0)";
    private static final String queryAggiornaStatisticheUtente = "UPDATE statistiche_utenti SET -?- = -?- + 1 WHERE username = ?";
    private static final String queryRegistraRicercaPartita = "INSERT INTO giocatori_disponibili VALUES (?, ?, ?)";
    private static final String queryRegistraRecuperoPartita = "INSERT INTO giocatori_in_attesa_recupero_partita VALUES (?, ?, ?)";
    private static final String queryRimuoviRicercaPartita = "DELETE FROM giocatori_disponibili WHERE username = ?";
    private static final String queryRimuoviRecuperoPartita = "DELETE FROM giocatori_in_attesa_recupero_partita WHERE username = ?";
    private static final String queryOttieniRicercaPartita = "SELECT username, indirizzo_ip, porta FROM giocatori_disponibili LIMIT 1";
    private static final String queryOttieniRecuperoPartita = "SELECT username, indirizzo_ip, porta FROM giocatori_in_attesa_recupero_partita WHERE username = ?";

    private static final String segnapostoColonne = "-?-";

    public static void impostaIndirizzoDatabase(String indirizzo, int porta)
    {
        OperazioniDatabase.indirizzoDatabase = indirizzo;
        OperazioniDatabase.porta = porta;
    }
    
    public static StatisticheUtente leggiStatisticheUtente(String usernameGiocatore) {
        try (
                Connection co = DriverManager.getConnection(("jdbc:" + tipoDatabase + "://" + indirizzoDatabase + ":" + porta + "/" + nomeSchema), "root", "");
                PreparedStatement ps = co.prepareStatement(queryLeggiStatisticheUtente);) {
            ps.setString(1, usernameGiocatore);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                StatisticheUtente st = new StatisticheUtente(rs.getInt("partite_vinte"), rs.getInt("partite_perse"));
                return st;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void registraNuovoUtente(String usernameGiocatore) {
        try (
                Connection co = DriverManager.getConnection(("jdbc:" + tipoDatabase + "://" + indirizzoDatabase + ":" + porta + "/" + nomeSchema), "root", "");
                PreparedStatement ps = co.prepareStatement(queryRegistraNuovoUtente);) {
            ps.setString(1, usernameGiocatore);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void aggiornaStatisticheUtente(String usernameGiocatore, boolean vittoria) {
        try (
                Connection co = DriverManager.getConnection(("jdbc:" + tipoDatabase + "://" + indirizzoDatabase + ":" + porta + "/" + nomeSchema), "root", "");) {
            String colonnaDaModificare = vittoria ? "partite_vinte" : "partite_perse";
            String query = queryAggiornaStatisticheUtente.replace(segnapostoColonne, colonnaDaModificare);
            PreparedStatement ps = co.prepareStatement(query);

            ps.setString(1, usernameGiocatore);

            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void registraRicercaPartita(RegistrazioneUtenteInAscolto registrazione) {
        try (
                Connection co = DriverManager.getConnection(("jdbc:" + tipoDatabase + "://" + indirizzoDatabase + ":" + porta + "/" + nomeSchema), "root", "");
                PreparedStatement ps = co.prepareStatement(queryRegistraRicercaPartita);) {
            ps.setString(1, registrazione.username);
            ps.setString(2, registrazione.indirizzoAscolto);
            ps.setInt(3, registrazione.portaAscolto);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void registraRecuperoPartita(RegistrazioneUtenteInAscolto registrazione) {
        try (
                Connection co = DriverManager.getConnection(("jdbc:" + tipoDatabase + "://" + indirizzoDatabase + ":" + porta + "/" + nomeSchema), "root", "");
                PreparedStatement ps = co.prepareStatement(queryRegistraRecuperoPartita);) {
            ps.setString(1, registrazione.username);
            ps.setString(2, registrazione.indirizzoAscolto);
            ps.setInt(3, registrazione.portaAscolto);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void rimuoviRicercaPartita(String usernameGiocatore) {
        try (
                Connection co = DriverManager.getConnection(("jdbc:" + tipoDatabase + "://" + indirizzoDatabase + ":" + porta + "/" + nomeSchema), "root", "");
                PreparedStatement ps = co.prepareStatement(queryRimuoviRicercaPartita);) {
            ps.setString(1, usernameGiocatore);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void rimuoviRecuperoPartita(String usernameGiocatore) {
        try (
                Connection co = DriverManager.getConnection(("jdbc:" + tipoDatabase + "://" + indirizzoDatabase + ":" + porta + "/" + nomeSchema), "root", "");
                PreparedStatement ps = co.prepareStatement(queryRimuoviRecuperoPartita);) {
            ps.setString(1, usernameGiocatore);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static RegistrazioneUtenteInAscolto ottieniRicercaPartita() {
        try (
                Connection co = DriverManager.getConnection(("jdbc:" + tipoDatabase + "://" + indirizzoDatabase + ":" + porta + "/" + nomeSchema), "root", "");
                PreparedStatement ps = co.prepareStatement(queryOttieniRicercaPartita);) {
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new RegistrazioneUtenteInAscolto(rs.getString("username"), rs.getString("indirizzo_ip"), rs.getInt("porta"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static RegistrazioneUtenteInAscolto ottieniRecuperoPartita(String usernameAvversario) {
        try (
                Connection co = DriverManager.getConnection(("jdbc:" + tipoDatabase + "://" + indirizzoDatabase + ":" + porta + "/" + nomeSchema), "root", "");
                PreparedStatement ps = co.prepareStatement(queryOttieniRecuperoPartita);) {
            ps.setString(1, usernameAvversario);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new RegistrazioneUtenteInAscolto(rs.getString("username"), rs.getString("indirizzo_ip"), rs.getInt("porta"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
