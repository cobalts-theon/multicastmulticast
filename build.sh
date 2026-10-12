#!/usr/bin/env sh
# Chuyen ve thu muc ma nguon va dat file .class trong build.
set -eu
cd "$(dirname "$0")"
mkdir -p build
javac -encoding UTF-8 -d build Net.java
echo "Build thanh cong: build/Net.class"
