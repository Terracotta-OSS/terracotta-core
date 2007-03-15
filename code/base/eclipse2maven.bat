@ECHO OFF
IF NOT EXIST eclipse2maven.rb GOTO no_eclips2maven

set JRUBY_HOME=%~dp0..\..\buildsystems\jruby

if NOT EXIST "%JRUBY_HOME%" GOTO install_jruby
GOTO has_jruby_home

:install_jruby
echo --------------------------------------------------------------------------------
echo LOADING JRUBY USING IVY

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
echo RUNNING ECLIPSE2MAVEN -- eclipse2maven.bat

GOTO run_jruby

:run_jruby

%JRUBY_HOME%\bin\jruby.bat -Ieclipse2maven eclipse2maven.rb %*
set TCBUILD_ERR=%ERRORLEVEL%
GOTO end

:no_eclipse2maven
    echo There is no eclipse2maven.rb file in this directory. Please run this script from a directory with an eclipse2maven.rb file.
    GOTO end

:end
REM echo eclipse2maven.bat: exit code is %TCBUILD_ERR%
exit /b %TCBUILD_ERR%
