@echo off

REM 
REM The contents of this file are subject to the Terracotta Public License Version
REM 2.0 (the "License"); You may not use this file except in compliance with the
REM License. You may obtain a copy of the License at 
REM 
REM      http://terracotta.org/legal/terracotta-public-license.
REM 
REM Software distributed under the License is distributed on an "AS IS" basis,
REM WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
REM the specific language governing rights and limitations under the License.
REM 
REM The Covered Software is Terracotta Platform.
REM 
REM The Initial Developer of the Covered Software is 
REM     Terracotta, Inc., a Software AG company
REM

REM This script can not run on its own, it must be invoked by usermanagement.bat or keychain.bat
if not defined cli_name (
  echo This script cannot be called directly, you should use usermanagement.bat or keychain.bat
  exit /b 1
)

set TC_INSTALL_DIR=%~d0%~p0..\..\..
set TC_INSTALL_DIR="%TC_INSTALL_DIR:"=%"
set COMMAND_MANAGER=${commandmanager}

if exist %TC_INSTALL_DIR%\server\bin\setenv.bat (
  call %TC_INSTALL_DIR%\server\bin\setenv.bat
)

if not defined JAVA_HOME (
  echo JAVA_HOME is not defined
  exit /b 1
)

set JAVA_HOME="%JAVA_HOME:"=%"
set root=%~d0%~p0
set root="%root:"=%"

set cli_home=%root%
set cli_runner="%cli_home:"=%..\lib\${management_cli_project}-${management-cli.version}.jar"
IF NOT EXIST %cli_runner% (
   set cli_runner="%cli_home:"=%..\lib\${management_cli_project}\${management_cli_project}-${management-cli.version}.jar"
)

set CMD_LINE_ARGS=
:setArgs
 if ""%1""=="""" goto doneSetArgs
 set CMD_LINE_ARGS=%CMD_LINE_ARGS% %1
 shift
 goto setArgs
:doneSetArgs

set java_opts=
if not "%cli_name%"=="__hidden__" (
  echo Terracotta Command Line Tools %cli_name:"=%
)
%JAVA_HOME%\bin\java -Dmanager=%COMMAND_MANAGER% -Xmx256m -XX:MaxPermSize=128m ^
 %java_opts% ^
 -cp %cli_runner% %main_class:"=% %CMD_LINE_ARGS%

exit /B
