@echo off

set CP=..\conf
for %%i in ("..\lib\*.jar") do call catenv.bat %%i
for %%i in ("..\dist\*.jar") do call catenv.bat %%i

set OPTS = -Djava.protocol.handler.pkgs=uk.ac.rdg.resc.jstyx.client.protocol

java %OPTS% -cp %CP% %*
