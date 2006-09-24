@echo off

REM Change to the drive and directory containing the config XML and DTD.
REM %~dp0 contains the drive and path of the directory on which this script lives.
REM This ensures that the DTD is loaded correctly.

set cwd = %CD%
cd /d %~dp0..\conf

call ..\bin\JStyxRun uk.ac.rdg.resc.jstyx.gridservice.server.SGSServer SGSconfig.xml %*

REM change back to the previous working directory
REM this doesn't work if you quit the batch file with Control-C - never gets run
echo %cwd%
cd /d %cwd%
