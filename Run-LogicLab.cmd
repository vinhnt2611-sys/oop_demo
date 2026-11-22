@echo off
setlocal
cd /d "%~dp0"
where java >nul 2>&1
if errorlevel 1 (
  echo Java 17+ is required. Install a JDK and add Java to PATH.
  pause
  exit /b 1
)
if not exist "dist\LogicLab.jar" (
  echo Missing dist\LogicLab.jar. Run build.cmd first.
  pause
  exit /b 1
)
java -jar "dist\LogicLab.jar"
if errorlevel 1 pause
