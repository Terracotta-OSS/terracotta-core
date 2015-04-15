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

set TC_INSTALL_DIR=%~d0%~p0..\..
set TC_INSTALL_DIR="%TC_INSTALL_DIR:"=%"

if exist %TC_INSTALL_DIR%\server\bin\setenv.bat (
  call %TC_INSTALL_DIR%\server\bin\setenv.bat
)
if not defined JAVA_HOME (
  echo Environment variable JAVA_HOME needs to be set
  exit /b 1
)

set JAVA_HOME="%JAVA_HOME:"=%"

rem Set BigMemory to the base directory of the BigMemory Distribution
@SET WD=%~d0%~p0
@SET BIGMEMORY=%WD%..\..

@rem setup the class path...
CALL "%WD%"..\bin\buildcp.bat
SET BIGMEMORY_CP=%TMP_CP%


%JAVA_HOME%\bin\java.exe -cp %BIGMEMORY_CP% -Xmx200m  -Dcom.tc.productkey.path="%BIGMEMORY%"/terracotta-license.key   com.bigmemory.samples.configprogrammatic.ProgrammaticBasedBigMemoryConfiguration
