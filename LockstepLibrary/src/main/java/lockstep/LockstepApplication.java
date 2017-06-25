/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lockstep;

import lockstep.messages.simulation.LockstepCommand;

/**
 * Bridge between the application and the lockstep library.
 * A LockstepClient will call this methods during its execution.
 */
public interface LockstepApplication {
    /**
     * Must read input from user, and return a Command object to be executed.
     * If there is no input in a frame a Command object must still be returned,
     * possibly representing the lack of an user input in the semantic of the application
     * 
     * @return the Command object collected in the current frame
     */
    abstract LockstepCommand readInput();


    /**
     * Must suspend the simulation execution due to a synchronization issue
     */
    abstract void suspendSimulation();
    
    /**
     * Must resume the simulation execution, as input are now synchronized
     */
    abstract void resumeSimulation();

    /**
     * Must get the command contained in the frame input and execute it
     * 
     * @param c the command to execute
     */
    abstract void executeCommand(LockstepCommand c);

    /**
     * Provides void commands to resume from a deadlock situation.
     * Their number should be dimensioned to take less time than the user to
     * react from the simulation being resumed
     * 
     * @return array of commands to help the simulation run smoothly
     */
    abstract LockstepCommand[] fillCommands();
        
    /**
     * Provides the first commands to bootstrap the simulation.
     * 
     * @return commands to bootstrap the simulation
     */
    abstract LockstepCommand[] bootstrapCommands();

    /**
     * Signals that the rest of the application that the handshake has failed.
     * After this call, the LockstepClient thread terminates.
     */
    abstract void signalHandshakeFailure();
    
    /**
     * Signals to the application the disconnection of one client.
     * Has the number of clients remaining, including the local client, as a par.
     * The LockstepClient thread will continue the execution afterwards.
     * 
     * @param remainingClients the number of clients remained, 
     * including the local client
     */
    abstract void signalDisconnection(int remainingClients);
}
