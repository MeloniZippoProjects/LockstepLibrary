$nClients = 4;
$serverAddress = "localhost";
$serverPort = 8000;
$clientFramerate = 60;
$clientTickrate = 60;
$serverTickrate = 20;
$maxUDPPayloadLength = 300;
$fillTimeout = 5000;
$maxExecutionDistance = 30;
$fillSize = 5;
$connectionTimeout = 2000;
$frameLimit = 3600;
$abortOnDisconnect = "true";
$waitOnClose = "true";

$log = gci -Filter "logs";
Remove-Item $log -Recurse;

start -FilePath powershell -ArgumentList ("java", "`"-Dlogfile=server`"", "-cp", ".\target\mosaic-1-jar-with-dependencies.jar",
     "mosaicsimulation.MosaicLockstepServer", "--serverPort=$serverPort", "--nClients=$nClients", "--tickrate=$serverTickrate", 
     "--maxUDPPayloadLength=$maxUDPPayloadLength", "--connectionTimeout=$connectionTimeout");

for($i = 0; $i -lt $nClients; $i++)
{
    start -FilePath powershell -ArgumentList ("java", "-Dlogfile=client_$i", "-cp", ".\target\mosaic-1-jar-with-dependencies.jar",
     "mosaicsimulation.MosaicSimulation", "--serverIPAddress=$serverAddress", "--serverTCPPort=$serverPort",
     "--framerate=$clientFramerate","--tickrate=$clientTickrate", "--maxUDPPayloadLength=$maxUDPPayloadLength",
     "--fillTimeout=$fillTimeout", "--maxExecutionDistance=$maxExecutionDistance", "--fillSize=$fillSize", "--frameLimit=$frameLimit",
     "--connectionTimeout=$connectionTimeout", "--abortOnDisconnect=$abortOnDisconnect", "--waitOnClose=$waitOnClose") 
}
