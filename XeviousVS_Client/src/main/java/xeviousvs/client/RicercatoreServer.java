/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package xeviousvs.client;

/**
 *
 * @author enric
 */
public class RicercatoreServer {
    
    public static ServerDisponibile ottieniServer()
    {
        return OperazioniDatabaseClient.ottieniServerDisponibile();
    }
    
}
