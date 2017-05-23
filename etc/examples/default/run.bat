REM This is a batch script that can be used to run product distribution
REM


start "Product Distribution" ^
	cmd /c java -jar ProductClient.jar --configFile=config.ini --receive

