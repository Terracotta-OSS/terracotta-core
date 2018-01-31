:: Copyright (c) 2011-2018 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
:: Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
@echo off
setlocal


set TC_VOTER_DIR=%~d0%~p0..
set TC_VOTER_DIR="%TC_VOTER_DIR:"=%"

if exist %TC_VOTER_DIR%\bin\setenv.bat (
  call %TC_VOTER_DIR%\bin\setenv.bat
)

if not defined JAVA_HOME (
  echo Environment variable JAVA_HOME needs to be set
  exit /b 1
)

set TC_KIT_ROOT="%TC_VOTER_DIR%\.."
set TC_KIT_ROOT="%TC_KIT_ROOT:"=%"
set TC_LOGGING_ROOT="%TC_KIT_ROOT%\client\logging"
set TC_LOGGING_ROOT="%TC_LOGGING_ROOT:"=%"
set TC_CLIENT_ROOT="%TC_KIT_ROOT%\client\logging"
set TC_CLIENT_ROOT="%TC_CLIENT_ROOT:"=%"

set CLASSPATH="%TC_VOTER_DIR%\lib\*;%TC_LOGGING_ROOT%\*;%TC_CLIENT_ROOT%\*;%TC_LOGGING_ROOT%\impl\*;%TC_LOGGING_ROOT%\impl\"
set CLASSPATH="%CLASSPATH:"=%"
set JAVA="%JAVA_HOME%\bin\java.exe"
set JAVA="%JAVA:"=%"

%JAVA% %JAVA_OPTS% -cp %CLASSPATH% org.terracotta.voter.TCVoterMain %*

exit /b %ERRORLEVEL%
endlocal
