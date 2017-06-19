$nClients = 4;

$serverAddress = "192.168.0.2";
$serverPort = 8000;
$clientFramerate = 60;
$clientTickrate = 30;
$fillTimeout = 5000;
$fillSize = 30;
$connectionTimeout = 2000;
$frameLimit = 2000;
$abortOnDisconnect = "true";
$waitOnClose = "true";


$log = gci -Filter "logs";
Remove-Item $log -Recurse;

for($i = 0; $i -lt $nClients; $i++)
{
    start -FilePath powershell -ArgumentList ("-noExit", "java", "-Dlogfile=client_$i", "-cp", ".\target\mosaic-1-jar-with-dependencies.jar", "mosaicsimulation.MosaicSimulation",
     "--serverIPAddress=$serverAddress", "--serverTCPPort=$serverPort", "--framerate=$clientFramerate","--tickrate=$clientTickrate", "--fillTimeout=$fillTimeout", 
     "--fillSize=$fillSize", "--frameLimit=$frameLimit", "--connectionTimeout=$connectionTimeout", "--abortOnDisconnect=$abortOnDisconnect", "--waitOnClose=$waitOnClose") 
}
