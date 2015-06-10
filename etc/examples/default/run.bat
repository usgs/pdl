REM This is a batch script that can be used to run product distribution
REM
REM $Id: run.bat 10673 2011-06-30 23:48:47Z jmfee $
REM $URL: https://ghttrac.cr.usgs.gov/websvn/ProductDistribution/trunk/etc/examples/default/run.bat $
REM



start "Product Distribution" ^
	cmd /c java -jar ProductClient.jar --configFile=config.ini --receive

