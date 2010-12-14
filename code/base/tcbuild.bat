@ECHO OFF

if "x%JAVA_HOME%"=="x" (
	echo "JAVA_HOME is not set"
  cmd /c exit /b 1
)

if "x%ANT_HOME%"=="x" (
	echo "ANT_HOME is not set"
  cmd /c exit /b 1
)

if "%1" == "help" (
  type README.txt
  exit /b 0
)

if "%1" == "" (
  type README.txt
  exit /b 0
)

setlocal enabledelayedexpansion

set jruby_version=jruby-1.1.6a
set findbugs_version=findbugs-1.3.9

set build_system_dir=%~dp0..\..\buildsystems
set old_CD=%CD%
cd %build_system_dir%
set build_system_dir=%CD%
cd %old_CD%
set cache_dir=c:\temp\tc
set JRUBY_HOME=%cache_dir%\%jruby_version%
set FINDBUGS_HOME=%cache_dir%\%findbugs_version%
set extra_jruby_cp=
FOR %%F IN (%build_system_dir%\jruby-lib\*.jar) DO (
  SET extra_jruby_cp=!extra_jruby_cp!;%%F%
)

if not exist "%cache_dir%" (
  mkdir %cache_dir%
)

if not exist "%JRUBY_HOME%" (
	echo JRuby not found at %JRUBY_HOME%. Installing...
	call %ANT_HOME%\bin\ant.bat -buildfile %build_system_dir%\install-tools.xml install-jruby
)

if not exist "%FINDBUGS_HOME%" (
	echo FindBugs not found at %FINDBUGS_HOME%. Installing...
	call %ANT_HOME%\bin\ant.bat -buildfile %build_system_dir%\install-tools.xml install-findbugs
)

set TC_CP=%extra_jruby_cp%

echo.
echo --------------------------------------------------------------------------------
echo RUNNING TCBUILD.BAT
echo.
call %JRUBY_HOME%\bin\jruby.bat -Ibuildscripts buildscripts\build-tc.rb %*
cmd /c exit /b %ERRORLEVEL%

endlocal
