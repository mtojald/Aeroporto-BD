#!/bin/sh
# Compila e roda a aplicacao (Linux/macOS/Git Bash). Requer JDK 17+.
cd "$(dirname "$0")" || exit 1
mkdir -p out
javac --release 17 -encoding UTF-8 -cp "lib/*" -d out $(find src -name '*.java') || exit 1
SEP=":"; case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=";";; esac
exec java -cp "out${SEP}lib/*" aeroporto.Main
