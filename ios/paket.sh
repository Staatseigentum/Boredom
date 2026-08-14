#!/usr/bin/env bash
#
# Baut aus dem, was Gradle erzeugt hat, eine unsignierte IPA.
#
# Ein iOS-Programm ist ein Ordner: ein Mach-O-Programm, eine Info.plist, ein paar Dateien
# daneben — und in einer IPA liegt dieser Ordner unter `Payload/`. Kotlin/Native baut das
# Programm, also braucht es hier weder ein Xcode-Projekt noch eine Zeile Swift; was fehlt,
# ist das Drumherum, und das steht hier.
#
# Signiert wird nichts. Genau darum geht es: wer die Datei installiert, signiert sie mit
# seiner eigenen Apple-ID auf seinem eigenen Gerät. Siehe README.
#
# Aufruf:  ios/paket.sh 4.2.0
set -euo pipefail

VERSION="${1:-0.0.0}"
# Nur Ziffern und Punkte — CFBundleShortVersionString verträgt nichts anderes, und ein `v`
# davor hat schon einmal ein ganzes Release gekostet.
VERSION="${VERSION#v}"

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BUILD="$ROOT/ios/build"
STAGE="$BUILD/paket"
APP="$STAGE/Payload/Kollaps.app"
DIST="$BUILD/dist"

PROGRAM="$BUILD/bin/iosArm64/releaseExecutable/Kollaps.kexe"
if [ ! -f "$PROGRAM" ]; then
  echo "Kein Programm gebaut: $PROGRAM" >&2
  echo "Erst  ./gradlew -c settings-ios.gradle.kts :ios:linkReleaseExecutableIosArm64" >&2
  exit 1
fi

rm -rf "$STAGE" "$DIST"
mkdir -p "$APP" "$DIST"

# Das Programm selbst. Der Name muss CFBundleExecutable unten entsprechen.
cp "$PROGRAM" "$APP/Kollaps"
chmod +x "$APP/Kollaps"

# Die Pixelschriften. Kotlin/Native kennt keinen Klassenpfad, also liegen sie als gewöhnliche
# Dateien im Bündel und werden über NSBundle wieder herausgesucht.
cp "$ROOT/app/src/main/res/font"/*.ttf "$APP/"

# ---------------------------------------------------------------- Symbol
#
# actool baut aus dem Katalog eine Assets.car und sagt in einer Teil-Plist, welche Schlüssel
# dafür in die Info.plist gehören. Schlägt es fehl, kostet das genau das Symbol auf dem
# Startbildschirm und nicht das Spiel — also darf es fehlschlagen.
ICONSET="$STAGE/Assets.xcassets/AppIcon.appiconset"
ICON_PLIST="$STAGE/symbol.plist"
mkdir -p "$ICONSET"
if [ -f "$BUILD/icon/AppIcon-1024.png" ]; then
  cp "$BUILD/icon/AppIcon-1024.png" "$ICONSET/AppIcon-1024.png"
  cat > "$ICONSET/Contents.json" <<'JSON'
{
  "images" : [
    {
      "filename" : "AppIcon-1024.png",
      "idiom" : "universal",
      "platform" : "ios",
      "size" : "1024x1024"
    }
  ],
  "info" : { "author" : "xcode", "version" : 1 }
}
JSON
  xcrun actool "$STAGE/Assets.xcassets" \
    --compile "$APP" \
    --platform iphoneos \
    --minimum-deployment-target 15.0 \
    --target-device iphone \
    --target-device ipad \
    --app-icon AppIcon \
    --output-partial-info-plist "$ICON_PLIST" \
    > /dev/null || echo "Warnung: kein Symbol gebaut — die App läuft trotzdem." >&2
else
  echo "Warnung: kein Symbol vorhanden (:ios:appIcon nicht gelaufen)." >&2
fi

# ---------------------------------------------------------------- Info.plist
#
# UILaunchScreen fehlt hier nicht zufällig als leeres Wörterbuch: ohne den Schlüssel startet
# iOS die App im Briefkastenformat einer alten Auflösung, mit schwarzen Balken oben und unten.
cat > "$APP/Info.plist" <<PLIST
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>CFBundleDevelopmentRegion</key>
    <string>de</string>
    <key>CFBundleDisplayName</key>
    <string>Kollaps</string>
    <key>CFBundleExecutable</key>
    <string>Kollaps</string>
    <key>CFBundleIdentifier</key>
    <string>com.staatseigentum.kollaps</string>
    <key>CFBundleInfoDictionaryVersion</key>
    <string>6.0</string>
    <key>CFBundleName</key>
    <string>Kollaps</string>
    <key>CFBundlePackageType</key>
    <string>APPL</string>
    <key>CFBundleShortVersionString</key>
    <string>${VERSION}</string>
    <key>CFBundleVersion</key>
    <string>${VERSION}</string>
    <key>LSRequiresIPhoneOS</key>
    <true/>
    <key>MinimumOSVersion</key>
    <string>15.0</string>
    <key>UIDeviceFamily</key>
    <array>
        <integer>1</integer>
        <integer>2</integer>
    </array>
    <key>UILaunchScreen</key>
    <dict/>
    <key>UIRequiredDeviceCapabilities</key>
    <array>
        <string>arm64</string>
    </array>
    <key>UIStatusBarStyle</key>
    <string>UIStatusBarStyleLightContent</string>
    <key>UISupportedInterfaceOrientations</key>
    <array>
        <string>UIInterfaceOrientationPortrait</string>
        <string>UIInterfaceOrientationLandscapeLeft</string>
        <string>UIInterfaceOrientationLandscapeRight</string>
    </array>
    <key>UIViewControllerBasedStatusBarAppearance</key>
    <false/>
</dict>
</plist>
PLIST

if [ -f "$ICON_PLIST" ]; then
  /usr/libexec/PlistBuddy -c "Merge $ICON_PLIST" "$APP/Info.plist" > /dev/null
fi
plutil -convert binary1 "$APP/Info.plist"

# ---------------------------------------------------------------- IPA
#
# Eine IPA ist ein ZIP mit `Payload/` darin. Mehr nicht — und ohne _CodeSignature, weil die
# Signatur beim Installieren entsteht und nicht hier.
( cd "$STAGE" && zip -qry "$DIST/Kollaps-${VERSION}-unsigniert.ipa" Payload )

ls -lh "$DIST"
echo "Fertig: $DIST/Kollaps-${VERSION}-unsigniert.ipa"
