#!/usr/bin/env bash

find . -type f \( \
  -name "*.java" -o \
  -name "*.yaml" -o \
  -name "*.yml" -o \
  -name "*.cfg" -o \
  -name "*.conf" -o \
  -name "*.ini" -o \
  -name "*.sh" -o \
  -name "*.j2" -o \
  -name "*.txt" -o \
  -name "*.properties" -o \
  -name "*.project" -o \
  -name "*.classpath" -o \
  -name "*.xml" -o \
  -name "*.md" -o \
  -name "*.ps1" -o \
  -name "*.bat" -o \
  -name "*.desktop" -o \
  -name "*.plist" -o \
  -name "*.wxs" -o \
  -name "*.wxi" -o \
  -name "postinstall" -o \
  -name "postrm" -o \
  -name "preinstall" -o \
  -name "prerm" \
\) \
  ! -name "context.txt" \
  ! -path "./.git/*" \
  ! -path "./output/*" \
  ! -path "./node_modules/*" \
  ! -path "./files/*" \
  ! -path "./repository/*" \
  ! -path "./.venv/*" \
  -print0 \
| sort -z \
| while IFS= read -r -d '' f; do
    echo "===== FILE: $f ====="
    echo
    cat "$f"
    echo
  done > context.txt