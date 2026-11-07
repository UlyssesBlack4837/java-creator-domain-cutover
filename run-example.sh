#!/usr/bin/env sh
set -eu
mkdir -p build
javac -d build $(find src/main/java -name '*.java')
java -cp build example.creatorcommerce.DomainCutoverApplication
