@echo off

REM %~dp0 gives the directory that contains this batch file.

REM make sure that this directory is in the classpath so that the log4j.properties
REM file is picked up
set CP="%~dp0;%~dp0..\build\web\WEB-INF\classes"
for %%i in ("%~dp0..\build\web\WEB-INF\lib\*.jar") do call catenv.bat %%i

java -cp %CP% uk.ac.rdg.resc.grex.client.cli.GRexRun %*