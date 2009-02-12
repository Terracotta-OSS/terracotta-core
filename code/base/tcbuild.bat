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

set jruby_version=jruby-1.1.2-20090208
set build_system_dir=%~dp0..\..\buildsystems
set cache_dir=c:\temp\tc
set JRUBY_HOME=%cache_dir%\%jruby_version%

if not exist "%JRUBY_HOME%" (
	echo JRuby not found at %JRUBY_HOME%. Installing...
	call %ANT_HOME%\bin\ant.bat -buildfile %build_system_dir%\install-jruby-build.xml
)

echo.
echo --------------------------------------------------------------------------------
echo RUNNING TCBUILD.BAT
echo.
call %JRUBY_HOME%\bin\jruby.bat -Ibuildscripts buildscripts\build-tc.rb %*
cmd /c exit /b %ERRORLEVEL%

endlocal
