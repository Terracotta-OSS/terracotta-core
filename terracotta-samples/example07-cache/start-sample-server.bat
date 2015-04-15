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


@SET WD=%~d0%~p0

rem Set the path to your Terracotta server home here
@SET TC_HOME=%WD%..\..\server


IF NOT EXIST "%TC_HOME%\bin\start-tc-server.bat" (
echo "Modify the script to set TC_HOME"
exit /B
)

start "terracotta" "%TC_HOME%\bin\start-tc-server.bat"

endlocal
