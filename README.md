# Bezahl- und Kommunikationsplatform

Eine Anwendung zum Versenden von virtuellem Geld und Schreiben von Direkt- und Pinnwandnachrichten.

## Gruppenmitglieder
- Hillenbrand, Arndt-Marius
- Kirchner, Colin
- Niedens, Vadim
- Renz, Daniel

## DB-Credentials (zur von Ihnen bereitgestellten DB)
- Host: kdb.sh
- Port: 6082
- Nutzer: fpj_2025_g1
- Datenbank: fpj_2025_g1
- Passwort: modulFPJ_Projekt_813

## Voraussetzungen
- Java 23
- PostgreSQL 
- Maven (maven wrapper enthalten) statt Gradle
- Docker zum Ausführen von DB-Integrationstests. Die Docker Engine muss gestartet werden, bevor die Tests ausgeführt werden.

## Setup & Start
- Das zip-Projekt muss entpackt und in eine IDE importiert werden
- das Maven Projekt muss geladen werden (in IntelliJ kommt ein Hinweis mit "Load Maven Project")
- mit den Credentials in der Datenbank einloggen oder mit den Dateien im Ordner src/main/resources/abgabezusatz die DB selbst aufsetzen
- (In der DML existieren bereits 3 User, mit denen interagiert werden kann. Sind diese nicht gewollt, kann nur das DDL-Skript ausgeführt werden.)
- wenn nicht die oben angegebene DB verwendet wird, müssen die DB-Properties/ -Credentials in der Datei src/main/resources/application.properties entsprechend verändert werden
- Zum Starten der App muss der Play-Button der Klasse App.java gedrückt werden, oder in bash:
```bash
mvn spring-boot:run
```
Wenn JavaFX bei diesem Befehl Probleme macht, dann ersatzweise dieser Befehl:
```bash
mvn javafx:run
