@echo off
cd /d %~dp0
title %~dp0[install]
@echo ************************** install *************************
call mvn clean install -e -U -Pdev -Dmaven.test.skip=true
pause