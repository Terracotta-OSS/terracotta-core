@echo off

if not defined JAVA_HOME (
  echo JAVA_HOME is not defined
  exit /b 1
)

setlocal

set JAVA_HOME="%JAVA_HOME:"=%"
set root=%~d0%~p0
set root="%root:"=%"

cd %root%
set tc_install_dir=..\..\..
rmdir /q /s dist
mkdir dist
xcopy /e /y /q web dist 1> NUL
mkdir dist\WEB-INF\classes 2> NUL
xcopy /e /y /q classes dist\WEB-INF\classes 1> NUL
mkdir dist\WEB-INF\lib 2> NUL

rem packaging terracotta-session
xcopy /y /q %tc_install_dir%\sessions\terracotta-session*.jar dist\WEB-INF\lib 1> NUL
if not %errorlevel% == 0  (
  echo "Couldn't package terracotta-session. Do you have a complete kit?"
  exit /b 1
)

rem create WAR
set warname=DepartmentTaskList.war
cd dist
%JAVA_HOME%\bin\jar cf %warname% *
if %errorlevel% == 0 (
  echo "%warname% has been created successfully."
  exit /b 0
) else (
  echo "Error packaging %warname%"
  exit /b 1
)

endlocal
