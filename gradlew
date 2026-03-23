#!/usr/bin/env sh

set -eu

APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
WRAPPER_DIR="$APP_HOME/gradle/wrapper"
WRAPPER_JAR="$WRAPPER_DIR/gradle-wrapper.jar"
WRAPPER_PROPS="$WRAPPER_DIR/gradle-wrapper.properties"
JAVA_EXE=${JAVA_HOME:+$JAVA_HOME/bin/java}
JAVA_EXE=${JAVA_EXE:-java}

bootstrap_wrapper_jar() {
  if [ -f "$WRAPPER_JAR" ]; then
    return 0
  fi

  mkdir -p "$WRAPPER_DIR"
  VERSION=$(sed -n 's#.*gradle-\([0-9.]*\)-bin.zip.*#\1#p' "$WRAPPER_PROPS" | head -n 1)
  if [ -z "${VERSION:-}" ]; then
    echo "Could not determine Gradle version from $WRAPPER_PROPS" >&2
    exit 1
  fi

  TMP_JAR="$WRAPPER_JAR.tmp"
  URL="https://services.gradle.org/distributions/gradle-${VERSION}-wrapper.jar"
  echo "Downloading missing Gradle wrapper jar for Gradle ${VERSION}..."

  if command -v curl >/dev/null 2>&1; then
    curl --fail --location --silent --show-error "$URL" --output "$TMP_JAR"
  elif command -v wget >/dev/null 2>&1; then
    wget -O "$TMP_JAR" "$URL"
  else
    echo "Neither curl nor wget is available to download $URL" >&2
    exit 1
  fi

  mv "$TMP_JAR" "$WRAPPER_JAR"
}

bootstrap_wrapper_jar
exec "$JAVA_EXE" -classpath "$WRAPPER_JAR" org.gradle.wrapper.GradleWrapperMain "$@"
