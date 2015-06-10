Hub Installation README


A Hub requires Apache and an EIDS Server


## EIDS Configuration

1) install EIDS server
	java -jar bin/EIDSInstaller.jar --install --server EIDS

2) Add a corba feeder to the EIDS server (for push notifications),
	in the file EIDS/conf/feeders.xml

<!--
CORBA FEEDER.
-->
	<QWFeederMod Name="corba"
			Class="com.isti.quakewatch.server.QWCorbaFeeder"
			LogFileName="log/corba.log"
			LogFileLevel="Info"
			ConsoleLevel="Debug">
		<QWFeederSettings>
			feederPortNumber = 38800
		</QWFeederSettings>
	</QWFeederMod>

3) make sure the [listener_sender] section of defaultHubConfig.ini
	matches your EIDS Server configuration


## Apache Configuration

1) Create an alias to the [listener_sender] storage ([sender_storage])
	directory.

2) Make sure the url of [sender_storage] refers to the alias and
	is accessible by all desired clients.
