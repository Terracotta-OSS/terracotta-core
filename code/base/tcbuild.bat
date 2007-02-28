@ECHO OFF
IF NOT EXIST build-tc.rb GOTO no_build_tc

set JRUBY_HOME=%~dp0..\..\buildsystems\jruby

if NOT EXIST "%JRUBY_HOME%" GOTO install_jruby
GOTO has_jruby_home

:install_jruby
echo --------------------------------------------------------------------------------
echo LOADING JRUBY USING IVY

call %ANT_HOME%\bin\ant.bat -buildfile %~p0buildconfig\build.xml

:has_jruby_home
if "x%JAVA_HOME%"=="x" GOTO has_no_java_home
GOTO has_java_home

:has_no_java_home
SET JAVA_HOME=%TC_JAVA_HOME_15%

:has_java_home
if NOT EXIST "%JAVA_HOME%" GOTO still_has_no_java_home
GOTO located_java_home

:still_has_no_java_home
echo Your JAVA_HOME (possibly located via TC_JAVA_HOME_15), "%JAVA_HOME%", does not exist. You must set this and re-run the script.
GOTO end

:located_java_home

echo --------------------------------------------------------------------------------
echo RUNNING TCBUILD -- tcbuild.bat

GOTO run_jruby

:run_jruby

%JRUBY_HOME%\bin\jruby.bat -Ibuildscripts build-tc.rb %*
set TCBUILD_ERR=%ERRORLEVEL%
GOTO end

:no_build_tc
	echo There is no build-tc.rb file in this directory. Please run this script from a directory with a build-tc.rb file.
	GOTO end

:end
REM echo tcbuild.bat: exit code is %TCBUILD_ERR%
exit /b %TCBUILD_ERR%
