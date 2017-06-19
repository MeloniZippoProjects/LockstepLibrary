$nClients = 4;

$serverPort = 8000;
$serverTickrate = 20;
$connectionTimeout = 2000;

$log = gci -Filter "logs";
Remove-Item $log -Recurse;

start -FilePath powershell -ArgumentList ("-noExit", "java", "`"-Dlogfile=server`"", "-cp", ".\target\mosaic-1-jar-with-dependencies.jar",
     "mosaicsimulation.MosaicLockstepServer", "--serverPort=$serverPort", "--nClients=$nClients", "--tickrate=$serverTickrate","--connectionTimeout=$connectionTimeout");
