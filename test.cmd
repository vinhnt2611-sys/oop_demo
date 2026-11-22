@echo off
setlocal
cd /d "%~dp0"
call build.cmd
if errorlevel 1 exit /b 1
if not exist build\tests mkdir build\tests
javac --release 17 -encoding UTF-8 -cp build\classes -d build\tests sourcecode\test\logic\EngineTest.java
if errorlevel 1 exit /b 1
java -Xmx512m -Djava.awt.headless=true -cp "build\classes;build\tests" logic.EngineTest
if errorlevel 1 exit /b 1
