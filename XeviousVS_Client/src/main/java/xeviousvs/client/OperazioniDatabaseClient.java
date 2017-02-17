package xeviousvs.client;

import java.sql.*;

public class OperazioniDatabaseClient {

    private static String indirizzoDatabase;
    private static int porta;

    private static final String tipoDatabase = "mysql";
    private static final String nomeSchema = "xeviousvs";
    private static final String usernameDatabase = "xevious";
    private static final String passwordDatabase = "xevious";

    private static final String queryLeggiStatisticheUtente = "SELECT partite_vinte, partite_perse FROM statistiche_utenti WHERE username = ?";
    private static final String queryRegistraNuovoUtente = "INSERT INTO statistiche_utenti VALUES (?, 0, 0)";
    private static final String queryAggiornaStatisticheUtente = "UPDATE statistiche_utenti SET -?- = -?- + 1 WHERE username = ?";
    private static final String queryOttieniRicercaPartita = "SELECT indirizzo_ip, porta FROM server_disponibili LIMIT 1";

    private static final String segnapostoColonne = "-?-";

    public static void impostaIndirizzoDatabase(String indirizzo, int porta)
    {
        OperazioniDatabaseClient.indirizzoDatabase = indirizzo;
        OperazioniDatabaseClient.porta = porta;
    }
    
    public static StatisticheUtente leggiStatisticheUtente(String usernameGiocatore) {
        try (
                Connection co = DriverManager.getConnection(("jdbc:" + tipoDatabase + "://" + indirizzoDatabase + ":" + porta + "/" + nomeSchema), usernameDatabase, passwordDatabase);
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
                Connection co = DriverManager.getConnection(("jdbc:" + tipoDatabase + "://" + indirizzoDatabase + ":" + porta + "/" + nomeSchema), usernameDatabase, passwordDatabase);
                PreparedStatement ps = co.prepareStatement(queryRegistraNuovoUtente);) {
            ps.setString(1, usernameGiocatore);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void aggiornaStatisticheUtente(String usernameGiocatore, boolean vittoria) {
        try (
                Connection co = DriverManager.getConnection(("jdbc:" + tipoDatabase + "://" + indirizzoDatabase + ":" + porta + "/" + nomeSchema), usernameDatabase, passwordDatabase);) {
            String colonnaDaModificare = vittoria ? "partite_vinte" : "partite_perse";
            String query = queryAggiornaStatisticheUtente.replace(segnapostoColonne, colonnaDaModificare);
            PreparedStatement ps = co.prepareStatement(query);

            ps.setString(1, usernameGiocatore);

            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static ServerDisponibile ottieniServerDisponibile() {
        try (
                Connection co = DriverManager.getConnection(("jdbc:" + tipoDatabase + "://" + indirizzoDatabase + ":" + porta + "/" + nomeSchema), usernameDatabase, passwordDatabase);
                PreparedStatement ps = co.prepareStatement(queryOttieniRicercaPartita);) {
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new ServerDisponibile(rs.getString("indirizzo_ip"), rs.getInt("porta"));
            }
            else return null;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
