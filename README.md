# Kollaps

Ein kleines Idle-Clicker-Spiel für Android. Du tippst nicht auf Kekse, sondern auf Himmelskörper:
angefangen beim Meteoriten kletterst du Stufe für Stufe die Leiter hoch — Asteroid, Merkur, Erde,
Saturn, Sonne, Neutronenstern — bis am Ende das Schwarze Loch steht.

Jede gesammelte Kilogramm Masse zählt. Sobald genug zusammenkommt, wird aus deinem Körper der
nächstgrößere, und der produziert dauerhaft mehr. Am Ende der Leiter kannst du **kollabieren**:
alles zurück auf Anfang, dafür Singularitäten, die jeden weiteren Durchlauf schneller machen.

## Was drin ist

- **18 Stufen** vom Meteoriten bis zum Schwarzen Loch, jede mit eigenem Aussehen und eigenem
  Produktionsmultiplikator
- **12 Kollektoren** als Idle-Produzenten, vom Staubfänger bis zum Urknall-Echo, mit
  Cookie-Clicker-typischer Preissteigerung von 15 % pro Stück
- **65 Upgrades**: Tipp-Verstärker, Verdopplungen pro Kollektor, globale Multiplikatoren,
  Offline-Verbesserungen
- **Offline-Produktion** — deine Kollektoren arbeiten weiter, während die App zu ist
  (standardmäßig 50 % für bis zu 8 Stunden, per Upgrade auf 100 % und 24 Stunden)
- **Prestige** über den Kollaps: Singularitäten geben je +10 % auf alles, dauerhaft
- **Kaufmengen** ×1 / ×10 / ×100 / Max
- Alle Himmelskörper werden **prozedural gezeichnet** — Krater, Wolkenbänder, Ringe, Korona,
  Akkretionsscheibe. Es gibt kein einziges Bild-Asset in der App.

## Aufbau

| Modul   | Inhalt |
| ------- | ------ |
| `core`  | Reines Kotlin, keine Android-Abhängigkeit: Spielregeln, Inhalte, Zahlenformatierung, Speicherformat |
| `app`   | Android-App mit Jetpack Compose: Rendering, Eingabe, Persistenz, Lebenszyklus |

Die Trennung ist Absicht: weil `core` nichts von Android weiß, lässt sich die komplette Simulation
in Unit-Tests im Schnelldurchlauf spielen. `BalanceSimulationTest` lässt einen Bot das Spiel
durchspielen und prüft damit die Progression, statt sie zu schätzen.

## Bauen

```bash
./gradlew :core:test          # Spiellogik testen
./gradlew :app:assembleDebug  # APK bauen
```

Das APK liegt danach unter `app/build/outputs/apk/debug/`. Der GitHub-Actions-Workflow
`.github/workflows/android.yml` baut es bei jedem Push und hängt es als Artefakt an den Lauf —
von dort lässt es sich direkt herunterladen und aufs Handy schieben.

Voraussetzungen: JDK 17 und ein Android SDK mit Plattform 35. Minimum ist Android 7.0 (API 24).

## Balance

Gemessen mit der Simulation aus `core/src/test`, gespielt von einem Bot, der nie vorausplant:

| Spielweise | Zeit bis zum Schwarzen Loch |
| ---------- | --------------------------- |
| aktiv (20 Minuten tippen, danach idle) | ~3 Stunden 10 Minuten |
| rein idle (eine Minute tippen, danach nur warten) | ~3 Stunden 30 Minuten |

Die erste Stufe kommt nach etwa einer Minute, die ersten sieben innerhalb der ersten Stunde.
