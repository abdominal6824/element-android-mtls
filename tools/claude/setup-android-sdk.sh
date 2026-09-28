#!/usr/bin/env bash
# Installe le SDK Android pour compiler Element dans une session Claude Code cloud.
# Idempotent : ne retélécharge rien si le SDK est déjà présent.
# Réseau requis (environnement "Custom") : dl.google.com, maven.google.com, repo1.maven.org,
# plugins.gradle.org, services.gradle.org, jitpack.io, oss.sonatype.org, s01.oss.sonatype.org
set -euo pipefail

ANDROID_HOME="${ANDROID_HOME:-$HOME/android-sdk}"
CMDLINE_TOOLS_ZIP="commandlinetools-linux-13114758_latest.zip"

if ! java -version 2>&1 | grep -q '"21'; then
    echo "JDK 21 requis" >&2
    exit 1
fi

if [ ! -x "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" ]; then
    mkdir -p "$ANDROID_HOME/cmdline-tools"
    tmp="$(mktemp -d)"
    curl -fsSL -o "$tmp/tools.zip" "https://dl.google.com/android/repository/$CMDLINE_TOOLS_ZIP"
    unzip -q "$tmp/tools.zip" -d "$tmp"
    mv "$tmp/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest"
    rm -rf "$tmp"
fi

export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"
yes | sdkmanager --licenses > /dev/null || true
sdkmanager "platform-tools" "platforms;android-36" "build-tools;36.0.0" > /dev/null

REPO_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
echo "sdk.dir=$ANDROID_HOME" > "$REPO_DIR/local.properties"

mkdir -p "$HOME/.gradle"
grep -q "org.gradle.jvmargs" "$HOME/.gradle/gradle.properties" 2>/dev/null || \
    echo "org.gradle.jvmargs=-Xmx6g -XX:MaxMetaspaceSize=1g" >> "$HOME/.gradle/gradle.properties"

echo "SDK Android prêt dans $ANDROID_HOME"
