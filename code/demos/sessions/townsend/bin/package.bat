@echo off

if not defined JAVA_HOME (
  echo JAVA_HOME is not defined
  exit /b 1
)

setlocal ENABLEDELAYEDEXPANSION

set JAVA_HOME="%JAVA_HOME:"=%"
set root=%~d0%~p0..
set root="%root:"=%"
set jetty1=%root%\..\jetty6.1\9081\webapps
set jetty2=%root%\..\jetty6.1\9082\webapps
cd %root%
set tc_install_dir=..\..\..
mkdir classes 2> NUL

for %%f in (..\..\..\ehcache\ehcache-core*.jar) do (
  set ehcache_core=%%f
)

if not exist %ehcache_core% (
  echo "Couldn't find ehcache-core jar. Do you have a full kit?"
  exit 1
)

set classpath=classes;%tc_install_dir%\lib\servlet-api-2.5-6.1.8.jar;%ehcache_core%

for %%f in (web\WEB-INF\lib\*.jar) do (
  set classpath=!classpath!;%%f
)

%JAVA_HOME%\bin\javac -d classes -sourcepath src -cp %classpath% src\demo\townsend\service\*.java src\demo\townsend\common\*.java src\demo\townsend\form\*.java src\demo\townsend\action\*.java

if not %errorlevel% == 0 ( 
  echo "Failed to compile demo. Do you have a full kit with Ehcache core?"
  exit /b 1
)

rmdir /q /s dist
mkdir dist
xcopy /e /y /q web dist 1> NUL
xcopy /e /y /q images dist 1> NUL
mkdir dist\WEB-INF\classes 2> NUL
xcopy /e /y /q classes dist\WEB-INF\classes 1> NUL
mkdir dist\WEB-INF\lib 2> NUL

rem packaging echcache-core
xcopy /y /q %ehcache_core% dist\WEB-INF\lib 1> NUL
if not %errorlevel% == 0  (
  echo "Couldn't package ehcache-core. Do you have a complete kit?"
  exit /b 1
)

rem packaging ehcache-terracotta
xcopy /y /q %tc_install_dir%\ehcache\ehcache-terracotta*.jar dist\WEB-INF\lib 1> NUL
if not %errorlevel% == 0  (
  echo "Couldn't package ehcache-terracotta. Do you have a complete kit?"
  exit /b 1
)

rem packaging terracotta-session
xcopy /y /q %tc_install_dir%\sessions\terracotta-session*.jar dist\WEB-INF\lib 1> NUL
if not %errorlevel% == 0  (
  echo "Couldn't package terracotta-session. Do you have a complete kit?"
  exit /b 1
)

rem create WAR
set warname=Townsend.war
cd dist
%JAVA_HOME%\bin\jar cf %warname% *
if %errorlevel% == 0 (
  echo "%warname% has been created successfully. Deploying..."
  xcopy /y /q %warname% %jetty1% 1> NUL
  xcopy /y /q %warname% %jetty2% 1> NUL
  echo "Done."
) else (
  echo "Error packaging %warname%"
  exit /b 1
)

endlocal
