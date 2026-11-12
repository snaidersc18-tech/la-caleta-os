#!/bin/sh
# Requiere JDK 21+ (con jpackage) y Maven en el PATH. Genera dist/MinisterioDelRebusque (Linux/macOS).
set -e
mvn -q clean package
rm -rf dist/MinisterioDelRebusque target/jpackage-input
mkdir -p target/jpackage-input
cp target/ministerio-del-rebusque-1.0.0-fat.jar target/jpackage-input/
jpackage --type app-image --name MinisterioDelRebusque --app-version 1.0.0 \
  --input target/jpackage-input --main-jar ministerio-del-rebusque-1.0.0-fat.jar --main-class app.main.Launcher \
  --dest dist --add-modules java.base,java.desktop,java.logging,java.xml,java.prefs,jdk.unsupported \
  --java-options "-Dfile.encoding=UTF-8"
mvn -q clean
echo "Listo: dist/MinisterioDelRebusque"
