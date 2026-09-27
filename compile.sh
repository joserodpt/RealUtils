#!/usr/bin/env bash
# Installs RealUtils into ~/.m2, where every Real* plugin's build picks it up.
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")"
mvn -q clean install
echo "Done: installed RealUtils into ~/.m2"
