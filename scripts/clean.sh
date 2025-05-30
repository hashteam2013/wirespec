#!/usr/bin/env bash

dir="$(dirname -- "$0")"
root="$dir/.."

docker rmi wirespec

./gradlew clean &&
  (cd "$root" && rm -rf kotlin-js-store) &&
  (cd "$root"/src/ide/vscode && npm run clean) &&
  (cd "$root"/src/site && make clean) &&
  (cd "$root"/examples/ && make clean) &&
  (cd "$root"/types && ./clean.sh) &&
  (cd "$root"/src/test && ./clean.sh)
