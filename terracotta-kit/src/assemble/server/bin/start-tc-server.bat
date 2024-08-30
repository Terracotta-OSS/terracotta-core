@REM
@REM  Copyright Terracotta, Inc.
@REM  Copyright Super iPaaS Integration LLC, an IBM Company 2024
@REM
@REM  Licensed under the Apache License, Version 2.0 (the "License");
@REM  you may not use this file except in compliance with the License.
@REM  You may obtain a copy of the License at
@REM
@REM      http://www.apache.org/licenses/LICENSE-2.0
@REM
@REM  Unless required by applicable law or agreed to in writing, software
@REM  distributed under the License is distributed on an "AS IS" BASIS,
@REM  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@REM  See the License for the specific language governing permissions and
@REM  limitations under the License.
@REM
@REM

@echo off

setlocal enabledelayedexpansion enableextensions

pushd "%~dp0.."
set "TC_SERVER_DIR=%CD%"
popd

if exist "!TC_SERVER_DIR!\bin\setenv.bat" (
  pushd "!TC_SERVER_DIR!\bin" && (
    call .\setenv.bat
    popd
  )
)

if not defined JAVA_HOME (
  echo Environment variable JAVA_HOME needs to be set
  exit /b 1
)

for %%C in ("-d64 -server -XX:MaxDirectMemorySize=1048576g" ^
			"-server -XX:MaxDirectMemorySize=1048576g" ^
			"-d64 -client  -XX:MaxDirectMemorySize=1048576g" ^
			"-client -XX:MaxDirectMemorySize=1048576g" ^
			"-XX:MaxDirectMemorySize=1048576g") ^
do (
  set JAVA_COMMAND="%JAVA_HOME%\bin\java" %%~C
  !JAVA_COMMAND! -version > NUL 2>&1

  if not errorlevel 1 (
	goto setJavaOptsAndClasspath
  ) else (
    echo [!JAVA_COMMAND!] failed - trying further options
  )
)
echo No executable Java environment found in [%JAVA_HOME%]
exit /b 1

:setJavaOptsAndClasspath

set OPTS=%SERVER_OPT% -Xms256m -Xmx2g -XX:+HeapDumpOnOutOfMemoryError
set OPTS=%OPTS% "-Dtc.install-root=%TC_SERVER_DIR%"
set JAVA_OPTS=%OPTS% %JAVA_OPTS%


:START_TCSERVER
%JAVA_COMMAND% %JAVA_OPTS% -jar "%TC_SERVER_DIR%\lib\tc.jar" %*
if %ERRORLEVEL% EQU 11 (
	echo start-tc-server: Restarting the server...
	goto START_TCSERVER
)
exit /b %ERRORLEVEL%
endlocal