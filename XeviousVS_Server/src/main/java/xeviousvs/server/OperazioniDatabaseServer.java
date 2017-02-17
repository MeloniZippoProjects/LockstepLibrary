package xeviousvs.server;

import java.sql.*;

public class OperazioniDatabaseServer
{

    private static String indirizzoDatabase;
    private static int portaDatabase;

    private static final String tipoDatabase = "mysql";
    private static final String nomeSchema = "xeviousvs";
    private static final String usernameDatabase = "xevious";
    private static final String passwordDatabase = "xevious";

    private static final String queryRegistraServerDisponibile = "INSERT INTO server_disponibili VALUES (?, ?, ?)";
    private static final String queryRimuoviServerDisponibile = "DELETE FROM server_disponibili WHERE id = ?";

    public static void impostaIndirizzoDatabase(String indirizzo, int porta)
    {
        OperazioniDatabaseServer.indirizzoDatabase = indirizzo;
        OperazioniDatabaseServer.portaDatabase = porta;
    }

    public static void registraServerDisponibile(int serverID, String indirizzoServer, int portaServer)
    {
        try (
                Connection co = DriverManager.getConnection(("jdbc:" + tipoDatabase + "://" + indirizzoDatabase + ":" + portaDatabase + "/" + nomeSchema), usernameDatabase, passwordDatabase);
                PreparedStatement ps = co.prepareStatement(queryRegistraServerDisponibile);)
        {
            ps.setInt(1, serverID);
            ps.setString(2, indirizzoServer);
            ps.setInt(3, portaServer);
            ps.executeUpdate();
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public static void rimuoviServerDisponibile(int serverID)
    {
        try (
                Connection co = DriverManager.getConnection(("jdbc:" + tipoDatabase + "://" + indirizzoDatabase + ":" + portaDatabase + "/" + nomeSchema), usernameDatabase, passwordDatabase);
                PreparedStatement ps = co.prepareStatement(queryRimuoviServerDisponibile);)
        {
            ps.setInt(1, serverID);
            ps.executeUpdate();
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }
}
