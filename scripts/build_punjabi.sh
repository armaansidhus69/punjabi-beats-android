#!/usr/bin/env bash
# Builds the Punjabi Beats (zen-integrated) debug APK, retrying through
# transient Maven Central / Google 429 rate limits.
set -u
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$ROOT_DIR/punjabi-beats"
if [ -d "$ROOT_DIR/jdk" ]; then
  export JAVA_HOME="$ROOT_DIR/jdk"
fi
LOG_FILE="$ROOT_DIR/build_punjabi.log"
for attempt in 1 2 3 4 5 6; do
  echo "=== attempt $attempt ==="
  ./gradlew :app:assembleDebug --no-daemon -q \
    > "$LOG_FILE" 2>&1
  code=$?
  if [ $code -eq 0 ]; then
    echo "BUILD OK on attempt $attempt"
    exit 0
  fi
  if grep -q "429\|Too Many Requests\|Could not GET\|Could not resolve\|daemon disappeared" "$LOG_FILE"; then
    echo "rate-limit/network/daemon failure; backing off 60s"
    sleep 60
  else
    echo "compile failure (exit $code)"
    exit $code
  fi
done
echo "ALL RETRIES EXHAUSTED"
exit 1
