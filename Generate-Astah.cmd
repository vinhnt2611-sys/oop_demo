@echo off
setlocal
cd /d "%~dp0"
set "ASTAH_INSTALL=%~1"
if not defined ASTAH_INSTALL set "ASTAH_INSTALL=%~dp0..\.tools\astah12\runtime\astah_uml"
if not exist "%ASTAH_INSTALL%\astah-api.jar" (
  echo Usage: Generate-Astah.cmd "path to installed Astah UML or Professional"
  exit /b 1
)
if not exist build\astah-generator mkdir build\astah-generator
javac -encoding UTF-8 -cp "%ASTAH_INSTALL%\astah-api.jar" -d build\astah-generator design\tools\CreateAstah.java
if errorlevel 1 exit /b 1
set "ASTAH_PRODUCT=%ASTAH_INSTALL%\astah-uml.jar"
if not exist "%ASTAH_PRODUCT%" set "ASTAH_PRODUCT=%ASTAH_INSTALL%\astah-pro.jar"
java -Duser.language=en -Dsun.java2d.d3d=false -cp "build\astah-generator;%ASTAH_INSTALL%\astah-api.jar;%ASTAH_PRODUCT%" CreateAstah "%~dp0dist\LogicLab.jar" "%~dp0design\LogicLab.asta"
if errorlevel 1 exit /b 1
