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

setlocal

if exist "%TC_VOTER_DIR%\bin\setenv.bat" (
  call "%TC_VOTER_DIR%\bin\setenv.bat"
)

if not defined JAVA_HOME (
  echo Environment variable JAVA_HOME needs to be set
  exit /b 1
)

set TC_KIT_ROOT=%TC_VOTER_DIR%\..
set TC_LOGGING_ROOT=%TC_KIT_ROOT%\client\logging
set TC_CLIENT_ROOT=%TC_KIT_ROOT%\client\lib

set CLASSPATH="%TC_VOTER_DIR%\lib\*;%TC_CLIENT_ROOT%\*;%TC_LOGGING_ROOT%\*;%TC_LOGGING_ROOT%\impl\*;%TC_LOGGING_ROOT%\impl"
set JAVA="%JAVA_HOME%\bin\java.exe"
set JAVA="%JAVA:"=%"

%JAVA% %JAVA_OPTS% -cp %CLASSPATH% %TC_VOTER_MAIN% %*

exit /b %ERRORLEVEL%

endlocal
