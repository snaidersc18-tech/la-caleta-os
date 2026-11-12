@echo off
REM Genera dist\MinisterioDelRebusque\MinisterioDelRebusque.exe (app-image portable: sin instalador ni WiX).
REM Usa mvn y jpackage del PATH; si no estan, los que trae Apache NetBeans (JDK 21+ con jpackage).
setlocal
set "NB=C:\Program Files\Apache NetBeans"
set "MVN=mvn"
set "JPK=jpackage"
where mvn >nul 2>nul || (set "MVN=%NB%\java\maven\bin\mvn.cmd" & set "JAVA_HOME=%NB%\jdk")
where jpackage >nul 2>nul || set "JPK=%NB%\jdk\bin\jpackage.exe"

call "%MVN%" -q clean package || exit /b 1

REM Solo el fat-jar entra al ejecutable (nada de classes/ ni restos de Maven).
if exist dist\MinisterioDelRebusque rmdir /s /q dist\MinisterioDelRebusque
mkdir target\jpackage-input
copy /y target\ministerio-del-rebusque-1.0.0-fat.jar target\jpackage-input\ >nul
"%JPK%" --type app-image --name MinisterioDelRebusque --app-version 1.0.0 ^
  --input target\jpackage-input --main-jar ministerio-del-rebusque-1.0.0-fat.jar --main-class app.main.Launcher ^
  --icon src\main\jpackage\icono.ico --dest dist ^
  --add-modules java.base,java.desktop,java.logging,java.xml,java.prefs,jdk.unsupported ^
  --java-options "-Dfile.encoding=UTF-8" || exit /b 1

REM Sin temporales de compilacion: target\ se borra al terminar.
call "%MVN%" -q clean
echo Listo: dist\MinisterioDelRebusque\MinisterioDelRebusque.exe
pause
