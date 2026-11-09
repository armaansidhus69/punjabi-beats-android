#!/usr/bin/env bash
# Restores the Android build toolchain after a sandbox wipe.
set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$ROOT_DIR"

echo "[1/4] JDK 17..."
if [ ! -x jdk/bin/java ]; then
  curl -sL -o jdk.tar.gz "https://api.adoptium.net/v3/binary/latest/17/ga/linux/x64/jdk/hotspot/normal/eclipse"
  mkdir jdk && tar -xzf jdk.tar.gz -C jdk --strip-components=1 && rm jdk.tar.gz
fi
jdk/bin/java -version 2>&1 | head -1

echo "[2/4] Android cmdline-tools..."
if [ ! -x android-sdk/cmdline-tools/latest/bin/sdkmanager ]; then
  curl -sL -o cmdtools.zip "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
  mkdir -p android-sdk/cmdline-tools
  unzip -q cmdtools.zip -d android-sdk/cmdline-tools
  mv android-sdk/cmdline-tools/cmdline-tools android-sdk/cmdline-tools/latest
  rm cmdtools.zip
fi

export ANDROID_HOME="$ROOT_DIR/android-sdk"
SDKM=android-sdk/cmdline-tools/latest/bin/sdkmanager
echo "[3/4] SDK packages (platform 34, build-tools 34)..."
yes | $SDKM --licenses >/dev/null 2>&1 || true
$SDKM --install "platform-tools" "platforms;android-34" "build-tools;34.0.0" >/dev/null

echo "[4/4] project local.properties..."
echo "sdk.dir=$ANDROID_HOME" > "$ROOT_DIR/punjabi-beats/local.properties"
ls android-sdk/platforms android-sdk/build-tools
echo "TOOLCHAIN READY"
