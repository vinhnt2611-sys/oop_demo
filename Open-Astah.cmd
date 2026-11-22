@echo off
setlocal
cd /d "%~dp0"
set "ASTAH_INSTALL=%~dp0..\.tools\astah12\runtime\astah_uml"
set "ASTAH_JAVA="
for /d %%D in ("%~dp0..\.tools\astah12\java25\*") do if exist "%%~fD\bin\javaw.exe" set "ASTAH_JAVA=%%~fD\bin\javaw.exe"
if not exist "%ASTAH_INSTALL%\astah-uml.jar" (
  echo Astah runtime was not found on this machine.
  echo Open design\LogicLab.asta with your installed Astah UML 12 or later.
  pause
  exit /b 1
)
if not defined ASTAH_JAVA (
  echo The local Java 25 runtime was not found.
  echo Open design\LogicLab.asta with your installed Astah UML 12 or later.
  pause
  exit /b 1
)
start "" "%ASTAH_JAVA%" -Xmx1024m -Duser.language=en -Dsun.java2d.d3d=false -jar "%ASTAH_INSTALL%\astah-uml.jar" "%~dp0design\LogicLab.asta"
