#!/usr/bin/env bash
# Compiles main + stubs + tests with -Xlint:all -Werror and runs each test's main().
set -euo pipefail
cd "$(dirname "$0")/.."
OUT=$(mktemp -d)
find stubs src/main/java src/test/java -name '*.java' > "$OUT/files.txt"
javac -Xlint:all -Werror -d "$OUT" @"$OUT/files.txt"
for t in $(cd src/test/java && find . -name '*Test.java' | sed 's|^\./||; s|\.java$||; s|/|.|g'); do
  java -cp "$OUT" "$t"
done
rm -rf "$OUT"
