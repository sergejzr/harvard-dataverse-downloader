#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$PROJECT_ROOT"

get_app_version() {
  local pom_version
  pom_version="$(sed -n '0,/<version>/s|.*<version>\(.*\)</version>.*|\1|p' pom.xml | head -n1)"
  if [[ -z "$pom_version" ]]; then
    echo "Could not read <version> from pom.xml" >&2
    exit 1
  fi

  if [[ "$pom_version" =~ ^([0-9]+)\.([0-9]+)\.([0-9]+) ]]; then
    echo "${BASH_REMATCH[1]}.${BASH_REMATCH[2]}.${BASH_REMATCH[3]}"
  else
    echo "Project version '$pom_version' is not a numeric version" >&2
    exit 1
  fi
}

if ! grep -q '<id>package-java</id>' pom.xml; then
  echo "ERROR: Maven profile 'package-java' is missing in pom.xml" >&2
  exit 1
fi

APP_VERSION="$(get_app_version)"
OUTPUT_BASE="$PROJECT_ROOT/output/java"
VERSION_OUTPUT_DIR="$OUTPUT_BASE/$APP_VERSION"
EXPECTED_JAR="$VERSION_OUTPUT_DIR/dataverse-downloader-all.jar"

if [ -x "$PROJECT_ROOT/mvnw" ]; then
  MVN="$PROJECT_ROOT/mvnw"
else
  MVN="mvn"
fi

echo "Using fat-jar profile: package-java"
echo "Resolved app version: $APP_VERSION"
echo "Output directory: $VERSION_OUTPUT_DIR"

rm -rf "$VERSION_OUTPUT_DIR"
mkdir -p "$VERSION_OUTPUT_DIR"

echo "Running Maven build..."
"$MVN" clean package -Ppackage-java "-Doutput.base=$VERSION_OUTPUT_DIR"

if [ ! -f "$EXPECTED_JAR" ]; then
  echo "ERROR: Build finished but fat jar was not created: $EXPECTED_JAR" >&2
  exit 1
fi

echo
echo "Fat JAR build finished successfully."
echo "Output written to: $VERSION_OUTPUT_DIR"
echo "Executable JAR: $EXPECTED_JAR"