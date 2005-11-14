@echo off

set CP="%JSTYX_HOME%\conf"
for %%i in ("%JSTYX_HOME%\lib\*.jar") do call catenv.bat %%i
for %%i in ("%JSTYX_HOME%\target\*.jar") do call catenv.bat %%i

set OPTS=-Djava.protocol.handler.pkgs=uk.ac.rdg.resc.jstyx.client.protocol

java %OPTS% -cp %CP% %*
