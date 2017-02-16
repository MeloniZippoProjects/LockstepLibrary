$serverAddress = "192.168.0.100";
$serverPort = 8000;
$databaseAddress = "localhost";
$databasePort = "3306";
$serverTickrate = 30;

$log = gci -Filter "logs";
Remove-Item $log -Recurse;

start -FilePath powershell -ArgumentList ("-noExit", "java", "`"-Dlogfile=xevserver`"", "-cp", ".\XeviousVS_Server\target\xeviousVS_Server-1-jar-with-dependencies.jar",
     "xeviousvs.server.XeviousVS_Server", "--serverAddress=$serverAddress","--serverPort=$serverPort", "--databaseAddress=$databaseAddress","--databasePort=$databasePort",
     "--tickrate=$serverTickrate");

