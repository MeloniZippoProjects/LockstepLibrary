$nClients = 1;

$serverAddress = "192.168.0.100";
$serverPort = 8000;
$clientFramerate = 60;
$clientTickrate = 40;
$fillTimeout = 500;
$fillSize = 50;

$log = gci -Filter "logs";
Remove-Item $log -Recurse;

for($i = 0; $i -lt $nClients; $i++)
{
    start -FilePath powershell -ArgumentList ("-noExit", "java", "-Dlogfile=client_$i", "-cp", ".\target\mosaic-1-jar-with-dependencies.jar", "mosaicsimulation.MosaicSimulation",
     "--serverIPAddress=$serverAddress", "--serverTCPPort=$serverPort", "--framerate=$clientFramerate","--tickrate=$clientTickrate", "--fillTimeout=$fillTimeout", "--fillSize=$fillSize") 
}
