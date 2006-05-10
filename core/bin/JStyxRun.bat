@echo off

REM %~dp0 gives the directory that contains this batch file.
REM Therefore there is no need to set %JSTYX_HOME%

set CP="%~dp0..\conf"
for %%i in ("%~dp0..\*.jar") do call catenv.bat %%i
for %%i in ("%~dp0..\lib\*.jar") do call catenv.bat %%i
for %%i in ("%~dp0..\target\*.jar") do call catenv.bat %%i

set OPTS=-Djava.protocol.handler.pkgs=uk.ac.rdg.resc.jstyx.client.protocol

java %OPTS% -cp %CP% %*
