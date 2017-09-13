# LockstepLibrary

This library was developed as group project for Concurrent and Distributed Systems course.

The idea behind this project was to implement first-hand a synchronization layer like most multiplayer games use nowadays.
The simulation (game) in object is collaborative effort where multiple clients generate inputs that drive it.
The synchronization layer has to guarantee that each client processes inputs in the correct order so that everyone has a consistent view of the simulation, without ever sharing its state directly.

We chose to implement the most strict version of lockstep, which doesn't allow for the simulation to go on speculatively in case of delays, contrary to most games.
We also chose the client-server structure, which should give the lowest delay in most cases.
In order to make the library as generic as possible, we chose to use Java Serialization for marshalling/unmarshalling.
This gives a huge overhead in terms of bandwidth, and should be reconsidered given the specific application needs.

The modularity of the library allows easy altering of these and other choices.

Two example applications are included which use the library:
- MosaicSimulation, which was used to test the library. 
  Each client is assigned a random color at initialization and then, at each frame, paints a random cell with its color.
  As the library guarantees synchronization, all clients have a consistent view of the mosaic when the simulation ends.
  
- XeviousVS, which is actually an adaptation of https://github.com/rzippo/XeviousVS which uses the library to avoid desynchronization issues.
  It must be noted that such issues are not completely solved in the current state of the repository, because of the algorithms used for computing damage, which can be affected by local computational delays. 
  In fact, split-second dodges are rarely correctly seen by the other client because of this.
