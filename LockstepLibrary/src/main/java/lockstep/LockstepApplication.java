/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lockstep;

import java.io.Serializable;

/**
 *
 * @author enric
 */
public interface LockstepApplication<Command extends Serializable> {
    /**
     * Must read input from user, and return a Command object to be executed.
     * If there is no input in a frame a Command object must still be returned,
     * possibly representing the lack of an user input in the semantic of the application
     * 
     * @return the Command object collected in the current frame
     */
    abstract Command readInput();


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
    abstract void executeCommand(Command c);

    /**
     * Provides void commands to resume from a deadlock situation.
     * Their number should be dimensioned to take less time than the user to
     * react from the simulation being resumed
     * 
     * @return array of commands to bootstart the simulation
     */
    abstract Command[] fillCommands();
        
    /**
     * Provides the first commands to bootstrap the simulation.
     * 
     * @return 
     */
    abstract Command[] bootstrapCommands();
    
}
