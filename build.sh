#!/usr/bin/env bash
# Build ProfileLimitUnlock.apk with plain Android SDK command-line tools.
set -euo pipefail
cd "$(dirname "$0")"

: "${ANDROID_SDK:?set ANDROID_SDK to your SDK root}"
BT="${BUILD_TOOLS:-$ANDROID_SDK/build-tools/34.0.0}"
AJAR="$ANDROID_SDK/platforms/android-34/android.jar"
API_JAR=tools/api-82.jar

mkdir -p tools build/classes build/dex
if [ ! -f "$API_JAR" ]; then
    curl -sL -o "$API_JAR" \
        "https://maven.aliyun.com/repository/jcenter/de/robv/android/xposed/api/82/api-82.jar"
fi

"$BT/aapt2" compile --dir res -o build/res.zip
"$BT/aapt2" link -o build/unsigned.apk -I "$AJAR" \
    --manifest AndroidManifest.xml -A assets build/res.zip

# Cross-platform classpath separator: ; on Windows, : on Unix
SEP=":"
[ "$(uname -s | cut -c1-5)" = "MINGW" ] && SEP=";"

# Use @argfile so Windows paths with drive letters survive Git Bash/MSYS mangling.
cat > build/javac_args.txt <<EOF
-encoding
UTF-8
-source
8
-target
8
-cp
$AJAR$SEP$API_JAR
-d
build/classes
src/com/agent/profilelimitunlock/HookEntry.java
src/com/agent/profilelimitunlock/MainActivity.java
EOF
javac @build/javac_args.txt

if [ -f "$BT/d8" ]; then D8=("$BT/d8")
else D8=(java -cp "$BT/lib/d8.jar" com.android.tools.r8.D8); fi
"${D8[@]}" --release --min-api 21 --lib "$AJAR" --classpath "$API_JAR" \
    --output build/dex build/classes/com/agent/profilelimitunlock/*.class

cp build/unsigned.apk build/withdex.apk
python3 - <<'EOF'
import zipfile
z = zipfile.ZipFile('build/withdex.apk', 'a')
z.write('build/dex/classes.dex', 'classes.dex', zipfile.ZIP_STORED)
z.close()
EOF

"$BT/zipalign" -f -p 4 build/withdex.apk build/aligned.apk

KS=build/debug.keystore
if [ ! -f "$KS" ]; then
    keytool -genkeypair -keystore "$KS" -alias androiddebugkey \
        -keyalg RSA -keysize 2048 -validity 10000 \
        -storepass android -keypass android -dname "CN=Android Debug"
fi
java -jar "$BT/lib/apksigner.jar" sign --ks "$KS" \
    --ks-pass pass:android --key-pass pass:android \
    --out ProfileLimitUnlock.apk build/aligned.apk

echo "OK: ProfileLimitUnlock.apk"
