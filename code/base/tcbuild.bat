@ECHO OFF

if "x%JAVA_HOME%"=="x" (
	echo "JAVA_HOME is not set"
  cmd /c exit /b 1
)

if "x%ANT_HOME%"=="x" (
	echo "ANT_HOME is not set"
  cmd /c exit /b 1
)

setlocal

set build_system_dir=%~dp0..\..\buildsystems
set JRUBY_HOME=%build_system_dir%\jruby-1.1.2-20090208

if not exist "%JRUBY_HOME%" (
	echo --------------------------------------------------------------------------------
	echo LOADING JRUBY USING IVY
	echo.
	call %ANT_HOME%\bin\ant.bat -buildfile %build_system_dir%\install-jruby-build.xml
)

echo.
echo --------------------------------------------------------------------------------
echo RUNNING TCBUILD.BAT
echo.
call %JRUBY_HOME%\bin\jruby.bat -Ibuildscripts buildscripts\build-tc.rb %*
cmd /c exit /b %ERRORLEVEL%

endlocal
