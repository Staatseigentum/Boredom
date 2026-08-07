# Kollaps

Ein kleines Idle-Clicker-Spiel für Android. Du tippst nicht auf Kekse, sondern auf Himmelskörper:
angefangen beim Meteoriten kletterst du Stufe für Stufe die Leiter hoch — Asteroid, Mond, Erde,
Saturn, Sonne, Hyperriese, Neutronenstern — bis am Ende das Schwarze Loch steht.

Jede gesammelte Kilogramm Masse zählt. Sobald genug zusammenkommt, wird aus deinem Körper der
nächstgrößere, und der produziert dauerhaft mehr. Am Ende der Leiter kannst du **kollabieren**:
alles zurück auf Anfang, dafür Singularitäten, die jeden weiteren Durchlauf schneller machen.

## Was drin ist

- **25 Stufen** vom Meteoriten bis zum Schwarzen Loch, jede mit eigenem Aussehen und eigenem
  Produktionsmultiplikator. Die letzten vier sind der Zusammenbruch: Hyperriese, Weißer Zwerg,
  Neutronenstern, Magnetar
- **17 Kollektoren** als Idle-Produzenten, vom Staubfänger bis zum Omega-Kollektor, mit
  Cookie-Clicker-typischer Preissteigerung von 15 % pro Stück
- **128 Upgrades**: Tipp-Verstärker, fünf Verdopplungsstufen pro Kollektor, globale
  Multiplikatoren, Offline-Verbesserungen — und **20 Synergien**, bei denen ein Kollektor einen
  anderen verstärkt. Sie sind die einzigen Upgrades, deren Stärke davon abhängt, wie die Flotte
  aussieht
- **Offline-Produktion** — deine Kollektoren arbeiten weiter, während die App zu ist
  (standardmäßig 50 % für bis zu 8 Stunden, per Upgrade auf 100 % und 48 Stunden)
- **Prestige** über den Kollaps: Singularitäten geben je +10 % auf alles, dauerhaft — und im
  Kosmos-Tab **13 Prestige-Upgrades**, die sie dauerhaft ausgeben
- **Kometen**, die alle paar Minuten durchs Bild ziehen und nur zahlen, wenn man sie trifft:
  geschenkte Produktion, ein Schub, oder eine Minute mit hundertfachem Tippwert
- **45 Erfolge**, jeder +1 % auf alles, dazu ein Statistik-Tab, der zeigt, welcher Kollektor
  gerade wie viel liefert
- **Meilensteine**: alle 25 Stück einer Sorte liefert dieser Kollektor 15 % mehr, dauerhaft und
  ohne Zusatzkosten — der Zähler in der Shop-Zeile zählt jetzt auf etwas zu
- **Vier Herausforderungen**: freiwillige Läufe unter einer Regel, die dir etwas wegnimmt —
  ohne Kollektoren, ohne Tippen, mit halber Kraft, oder gegen die Uhr. Wer sie besteht, behält
  die Belohnung für immer
- **Der Urknall**: ab zehn Kollapsen kannst du alles wegwerfen, was die Kollapse aufgebaut
  haben, und bekommst dafür **Äonen** — eine Währung unter der Singularität, mit acht eigenen
  Upgrades
- **Ereignisse mit Wahl**: alle paar Minuten eine Frage mit zwei Antworten. Masse sofort, oder
  ein Bonus auf Zeit — was besser ist, hängt davon ab, ob du gleich weiterspielst
- **Klang** für Stufenaufstieg, gefangenen Kometen, Kauf und bestandene Herausforderung, alles
  im Code aus Rechteckwellen synthetisiert statt als Audiodatei mitgeliefert
- **Produktionsverlauf** als Kurve über die letzte halbe Stunde, logarithmisch, weil eine lineare
  Achse in einem Idle-Spiel neunundzwanzig Minuten flach und dann senkrecht wäre
- **Zweispaltig im Querformat** und auf Tablets: Körper links, Shop rechts
- **Erinnerung**, wenn der Offline-Speicher voll gelaufen ist — genau dann, wenn die Kollektoren
  aufhören zu verdienen
- **Automatischer Tipper** aus dem Prestige-Shop: drei bzw. zehn Tipps pro Sekunde, die zählen
  wie deine eigenen — und ein **automatischer Kauf**, der erst zuschlägt, wenn du das Vierfache
  des Preises übrig hast, damit er dir nicht das Geld für Upgrades wegnimmt
- Deine Kollektoren **kreisen sichtbar** um den Körper, eine Bahn pro Sorte
- **Spielstand exportieren und einlesen** — die App ist sideloaded, also liegt der Stand nur auf
  dem Gerät und geht mit einer Deinstallation verloren
- **Kaufmengen** ×1 / ×10 / ×100 / Max
- Durchgehend **Pixel Art**: jeder Himmelskörper ist ein Sprite mit 24 Rotationsframes, gerendert
  aus einer Kugelprojektion über eine Oberflächentextur, auf eine Handvoll Palettenstufen
  quantisiert und mit einer 8×8-Bayer-Matrix gedithert. Krater, Wolkenbänder mit Sturm, Eiskappen,
  Ringe, Sterngranulation, Pulsar-Jets und Akkretionsscheibe entstehen alle im Code — es gibt kein
  einziges gemaltes Bild-Asset, auch das Launcher-Icon fällt aus demselben Renderer.
- Die **Auflösung wächst mit der Stufe**, von 96 px beim Meteoriten bis 288 px beim Hyperriesen
  und beim Schwarzen Loch. Eine feste Auflösung für alle geht nicht: Sprites werden ganzzahlig vergrößert, also
  landen kleine Körper bei Faktor 1 und verlieren ihre Pixelblöcke ganz, während große auf
  Faktor 2 zurückfallen müssten. So bleibt der Faktor über die ganze Leiter gleich — die Pixel
  sind im ganzen Spiel gleich groß, und das Detail wächst mit dem, was man tatsächlich sieht.
- **Zwei Pixel-Schriften**, weil das Spiel zwei Dinge von seinem Text verlangt: Silkscreen für
  Zähler, Überschriften und Knöpfe — kurz, laut, ohnehin in Großbuchstaben — und VT323 für
  Fließtext, das echte Kleinbuchstaben hat und in den Shop-Zeilen lesbar bleibt. Beide unter
  der SIL Open Font License, die Lizenztexte liegen unter `app/src/main/assets/licenses/`.
- **Klickgeräusch** auf dem Himmelskörper und auf allem, was im Menü anklickbar ist, gespielt
  über einen `SoundPool` — der dekodiert die Probe einmal beim Start, damit ein Tipp nie auf
  einen Decoder wartet, und lässt mehrere Instanzen überlappen, weil in einem Idle-Spiel
  schneller getippt wird als die Probe lang ist
- **Eingebauter Updater**: die App sieht selbst nach, ob eine neuere Version veröffentlicht
  wurde, lädt sie herunter und übergibt sie an den System-Installer

## Aufbau

| Modul     | Inhalt |
| --------- | ------ |
| `core`    | Reines Kotlin, keine Android-Abhängigkeit: Spielregeln, Inhalte, Zahlenformatierung, Speicherformat, Sprite-Renderer |
| `app`     | Android-App mit Jetpack Compose: Rendering, Eingabe, Persistenz, Lebenszyklus |
| `desktop` | Testfassung am Rechner. Führt **dieselbe** Oberfläche aus wie die App |

Die Trennung ist Absicht: weil `core` nichts von Android weiß, lässt sich die komplette Simulation
in Unit-Tests im Schnelldurchlauf spielen. `BalanceSimulationTest` lässt einen Bot das Spiel
durchspielen und prüft damit die Progression, statt sie zu schätzen. Aus demselben Grund liegt
auch der Sprite-Renderer dort: `PixelPlanet` gibt rohe Pixelpuffer zurück, die die App zu Bitmaps
macht — dadurch lassen sich die Planeten ohne Gerät rendern und in `PixelPlanetTest` prüfen.

## Am Rechner spielen

```bash
./gradlew --configure-on-demand :desktop:run              # von vorn
./gradlew --configure-on-demand :desktop:run --args="--tier 23"   # direkt beim Neutronenstern
```

Das ist kein Nachbau: `desktop` kompiliert die Oberflächen-Quellen direkt aus `app`. Möglich ist
das, weil Compose Multiplatform dieselbe `androidx.compose.*`-API veröffentlicht wie die
Android-Artefakte — ein Quelltext genügt beiden. Ausgenommen sind nur die wirklich
Android-gebundenen Dateien, und die sind einzeln aufgezählt statt per Muster, damit eine neue
Datei eine Entscheidung erzwingt statt still zu verschwinden.

Ohne Bildschirm geht es auch. Der folgende Aufruf rendert echte Spielbildschirme als PNG:

```bash
./gradlew --configure-on-demand :desktop:run \
  -PmainClass=com.staatseigentum.kollaps.desktop.ScreenshotsKt --args="build/screenshots"
```

Der CI-Lauf macht genau das bei jedem Push und hängt die Bilder als Artefakt an. Es ist der
einzige automatische Blick auf die Oberfläche, den das Projekt hat — sonst zeichnet sie nichts.
Gefunden hat der Harness unter anderem den Fehler, bei dem nach einem Stufenwechsel das Sprite
der *vorherigen* Stufe angezeigt und dabei auf die Kantenlänge der neuen zugeschnitten wurde.

## Bauen

```bash
./gradlew :core:test          # Spiellogik testen
./gradlew :app:assembleDebug  # APK bauen
```

Das APK liegt danach unter `app/build/outputs/apk/debug/`. Der GitHub-Actions-Workflow
`.github/workflows/android.yml` baut es bei jedem Push und hängt es als Artefakt an den Lauf —
von dort lässt es sich direkt herunterladen und aufs Handy schieben.

Voraussetzungen: JDK 17 und ein Android SDK mit Plattform 35. Minimum ist Android 7.0 (API 24).

## Updates

Weil die App nicht über einen Store läuft, kümmert sie sich selbst um Aktualisierungen. Sie fragt
höchstens alle sechs Stunden die Release-Liste des eigenen Repositories ab, vergleicht die
Versionsnummern und meldet sich nur, wenn wirklich etwas Neueres da ist. Der Download landet im
Cache-Verzeichnis; installiert wird nichts von allein — den letzten Schritt bestätigt immer der
System-Installer. Von Hand prüfen lässt sich jederzeit im Reiter **Kosmos**.

### Neue Version veröffentlichen

```bash
git tag v1.1.0 && git push origin v1.1.0
```

Das startet `.github/workflows/release.yml`: Tests laufen, das APK wird mit der Version aus dem
Tag gebaut, signiert, geprüft und als GitHub-Release samt APK veröffentlicht. Die Versionsnummer
kommt komplett aus dem Tag — auch der `versionCode`, den Android braucht, um den Build überhaupt
als Update zu akzeptieren (`1.2.3` wird zu `10203`).

### Was dafür einmalig eingerichtet werden muss

**1. Ein Signaturschlüssel als Repository-Secret.** Android ersetzt eine installierte App nur
durch eine mit derselben Signatur. Der Schlüssel muss also über alle Releases hinweg derselbe
bleiben — geht er verloren, lässt sich keine Aktualisierung mehr ausliefern.

```bash
keytool -genkeypair -v -keystore kollaps.jks -keyalg RSA -keysize 4096 \
        -validity 10000 -alias kollaps
base64 -w0 kollaps.jks    # Ausgabe als Secret KEYSTORE_BASE64 hinterlegen
```

Vier Secrets unter *Settings → Secrets and variables → Actions*: `KEYSTORE_BASE64`,
`KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`. Fehlen sie, bricht der Release-Workflow mit
einem Hinweis ab, statt ein nicht installierbares APK zu veröffentlichen.

**2. Das Repository muss öffentlich sein.** Der Updater fragt die Release-API ohne Anmeldung ab.
Solange `Boredom` privat ist, bekommt er ein 404 und zeigt „Keine Veröffentlichungen gefunden.
Ist das Repository öffentlich?". Ein Zugriffstoken ins APK zu legen wäre keine Lösung — das APK
kann jeder auslesen. Alternative, falls das Repo privat bleiben soll: die Releases woanders
hosten und die Konstante `LATEST_RELEASE_URL` in `UpdateService.kt` dorthin zeigen lassen. Außer
dieser einen Konstante weiß nichts in der App etwas von GitHub.

### Grenzen

Der Updater ersetzt nur **Release-Builds**. Das Debug-APK aus dem normalen CI-Lauf hat eine
andere Anwendungs-ID (`…kollaps.debug`) und eine andere Signatur — darüber lässt sich kein
Release installieren. Zum Spielen also einmal das APK von der Releases-Seite installieren, ab
dann hält sich die App selbst aktuell.

## Balance

Gemessen mit der Simulation aus `core/src/test`, gespielt von einem Bot, der nie vorausplant:

| Spielweise | Zeit bis zum Schwarzen Loch |
| ---------- | --------------------------- |
| aktiv (20 Minuten tippen, danach idle) | ~3 Stunden 57 Minuten |
| rein idle (eine Minute tippen, danach nur warten) | ~4 Stunden 13 Minuten |

Die erste Stufe kommt nach etwa einer Minute, die ersten sechs innerhalb der ersten Stunde. Der
Test verankert die Vier-Stunden-Marke, damit eine spätere Änderung an Preisen oder Schwellen die
Progression nicht unbemerkt verschiebt.
