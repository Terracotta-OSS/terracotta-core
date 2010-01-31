@echo off

if not defined JAVA_HOME (
  echo JAVA_HOME is not defined
  exit /b 1
)

setlocal

set samples_dir=%~d0%~p0..
set samples_dir="%samples_dir:"=%"

set jetty1=%samples_dir%\jetty6.1\9081\webapps
set jetty2=%samples_dir%\jetty6.1\9082\webapps

cd %samples_dir%

for %%d in (cart tasklist townsend) do (
  call %%d\package.bat
  xcopy /e /y /q %%d\dist\*.war %jetty1% 1> NUL
  xcopy /e /y /q %%d\dist\*.war %jetty2% 1> NUL
)

endlocal
