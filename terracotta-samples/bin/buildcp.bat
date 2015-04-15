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

SET QEB=%BIGMEMORY%\code-samples\bin

SET TMP_CP=.

dir /b "%BIGMEMORY%\code-samples\lib\*.jar" > temp.tmp
FOR /F %%I IN (temp.tmp) DO CALL "%QEB%\addpath.bat" "%BIGMEMORY%\code-samples\lib\%%I"

dir /b "%BIGMEMORY%\apis\ehcache\lib\*.jar" > temp.tmp
FOR /F %%I IN (temp.tmp) DO CALL "%QEB%\addpath.bat" "%BIGMEMORY%\apis\ehcache\lib\%%I"

dir /b "%BIGMEMORY%\apis\toolkit\lib\*.jar" > temp.tmp
FOR /F %%I IN (temp.tmp) DO CALL "%QEB%\addpath.bat" "%BIGMEMORY%\apis\toolkit\lib\%%I"

DEL temp.tmp
