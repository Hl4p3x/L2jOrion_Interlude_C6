@echo off
title L2jOrion: Game Server Console
:start

java -Djava.util.logging.manager=l2jorion.util.L2LogManager -Xms2g -Xmx2g -cp ./../libs/*;l2jorion-core.jar l2jorion.game.GameServer

if ERRORLEVEL 7 goto telldown
if ERRORLEVEL 6 goto tellrestart
if ERRORLEVEL 5 goto taskrestart
if ERRORLEVEL 4 goto taskdown
REM 3 - abort
if ERRORLEVEL 2 goto restart
if ERRORLEVEL 1 goto error
goto end
:tellrestart
echo.
echo Telnet server Restart ...
echo Send your bug to: https://www.l2jorion.com
echo.
goto start
:taskrestart
echo.
echo Auto Task Restart ...
echo Send your bug to: https://www.l2jorion.com
echo.
goto start
:restart
echo.
echo Admin Restart ...
echo Send your bug to: https://www.l2jorion.com
echo.
goto start
:taskdown
echo .
echo Server terminated (Auto task)
echo Send your bug to: https://www.l2jorion.com
echo .
:telldown
echo .
echo Server terminated (Telnet)
echo Send your bug to: https://www.l2jorion.com
:error
echo.
echo Server terminated abnormally
echo Send your bug to: https://www.l2jorion.com
echo.
:end
echo.
echo Server terminated
echo Send your bug to: https://www.l2jorion.com
echo.
:question
set choix=q
set /p choix=Restart(r) or Quit(q)
if /i %choix%==r goto start
if /i %choix%==q goto exit
:exit
exit
pause
