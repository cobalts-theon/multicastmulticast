#!/usr/bin/env sh
# Build truoc khi chay de luon dung ma nguon moi nhat.
set -eu
cd "$(dirname "$0")"
sh ./build.sh
exec java -cp build Net "$@"
