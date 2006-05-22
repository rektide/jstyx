@echo off

REM Change to the directory containing the config XML and DTD.
REM %~dp0 contains the directory from which this script runs.
REM This ensures that the DTD is loaded correctly.

cd %~dp0..\conf

call JStyxRun uk.ac.rdg.resc.jstyx.gridservice.server.StyxGridService SGSconfig.xml
