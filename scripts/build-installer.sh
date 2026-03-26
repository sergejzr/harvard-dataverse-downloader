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
    echo "Project version '$pom_version' is not a jpackage-compatible numeric version" >&2
    exit 1
  fi
}

APP_VERSION="$(get_app_version)"
OS="$(uname -s)"

case "$OS" in
  Darwin)
    PROFILE="installer-mac-dmg"
    PLATFORM="mac"
    ;;
  Linux)
    PROFILE="installer-linux-deb"
    PLATFORM="linux"
    ;;
  *)
    echo "Unsupported OS: $OS" >&2
    exit 1
    ;;
esac

OUTPUT_BASE="$PROJECT_ROOT/output/$PLATFORM"
VERSION_OUTPUT_DIR="$OUTPUT_BASE/$APP_VERSION"

if [ -x "$PROJECT_ROOT/mvnw" ]; then
  MVN="$PROJECT_ROOT/mvnw"
else
  MVN="mvn"
fi

echo "Using installer profile: $PROFILE"
echo "Resolved app version: $APP_VERSION"
echo "Output directory: $VERSION_OUTPUT_DIR"

rm -rf "$VERSION_OUTPUT_DIR"
mkdir -p "$VERSION_OUTPUT_DIR"

echo "Running Maven build..."
"$MVN" clean package "-P$PROFILE" "-Doutput.base=$VERSION_OUTPUT_DIR"

echo
echo "Installer build finished successfully."
echo "Output written to: $VERSION_OUTPUT_DIR"