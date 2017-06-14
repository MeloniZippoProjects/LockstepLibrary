$nClients = 2;
$serverAddress = "localhost";
$serverPort = 8000;
$clientFramerate = 60;
$clientTickrate = 30;
$serverTickrate = 30;
$fillTimeout = 5000;
$fillSize = 30;

$log = gci -Filter "logs";
Remove-Item $log -Recurse;

start -FilePath powershell -ArgumentList ("-noExit", "java", "`"-Dlogfile=server`"", "-cp", ".\target\mosaic-1-jar-with-dependencies.jar",
     "mosaicsimulation.MosaicLockstepServer", "--serverPort=$serverPort", "--nClients=$nClients", "--tickrate=$serverTickrate");
for($i = 0; $i -lt $nClients; $i++)
{
    start -FilePath powershell -ArgumentList ("-noExit", "java", "-Dlogfile=client_$i", "-cp", ".\target\mosaic-1-jar-with-dependencies.jar", "mosaicsimulation.MosaicSimulation",
     "--serverIPAddress=$serverAddress", "--serverTCPPort=$serverPort", "--framerate=$clientFramerate","--tickrate=$clientTickrate", "--fillTimeout=$fillTimeout", "--fillSize=$fillSize") 
}
