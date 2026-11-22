@echo off
setlocal
cd /d "%~dp0"
where javac >nul 2>&1
if errorlevel 1 (
  echo JDK 17+ is required. javac was not found in PATH.
  exit /b 1
)
if not exist build\classes mkdir build\classes
if not exist dist mkdir dist
javac --release 17 -encoding UTF-8 -d build\classes -sourcepath sourcecode\src sourcecode\src\logic\Main.java
if errorlevel 1 exit /b 1
jar --create --file dist\LogicLab.jar --main-class logic.Main -C build\classes .
if errorlevel 1 exit /b 1
echo Built dist\LogicLab.jar
