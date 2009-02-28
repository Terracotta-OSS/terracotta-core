@ECHO OFF

if "x%JAVA_HOME%"=="x" (
	echo "JAVA_HOME is not set"
  cmd /c exit /b 1
)

if "x%ANT_HOME%"=="x" (
	echo "ANT_HOME is not set"
  cmd /c exit /b 1
)

setlocal enabledelayedexpansion

set jruby_version=jruby-1.1.6
set build_system_dir=%~dp0..\..\buildsystems
set old_CD=%CD%
cd %build_system_dir%
set build_system_dir=%CD%
cd %old_CD%
set cache_dir=c:\temp\tc
set JRUBY_HOME=%cache_dir%\%jruby_version%
set extra_jruby_cp=%CLASSPATH%
FOR %%F IN (%build_system_dir%\jruby-lib\*.jar) DO (
  SET extra_jruby_cp=!extra_jruby_cp!;%%F%
)

if not exist "%cache_dir%" (
  mkdir %cache_dir%
)

if not exist "%JRUBY_HOME%" (
	echo JRuby not found at %JRUBY_HOME%. Installing...
	call %ANT_HOME%\bin\ant.bat -buildfile %build_system_dir%\install-jruby-build.xml
)

set CLASSPATH=%extra_jruby_cp%

echo.
echo --------------------------------------------------------------------------------
echo RUNNING TCBUILD.BAT
echo.
call %JRUBY_HOME%\bin\jruby.bat -Ibuildscripts buildscripts\build-tc.rb %*
cmd /c exit /b %ERRORLEVEL%

endlocal
