@echo off

REM Change to the drive and directory containing the config XML and DTD.
REM %~d0 contains the drive (e.g. c:) on which this script lives.
REM %~dp0 contains the drive and path of the directory on which this script lives.
REM This ensures that the DTD is loaded correctly.

%~d0
cd %~dp0..\conf

call JStyxRun uk.ac.rdg.resc.jstyx.gridservice.server.SGSServer SGSconfig.xml %*
