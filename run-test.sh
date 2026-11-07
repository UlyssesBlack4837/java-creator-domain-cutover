#!/usr/bin/env sh
set -eu
mkdir -p build-test
javac -d build-test $(find src/main/java src/test/java -name '*.java')
java -cp build-test example.creatorcommerce.DomainCutoverServiceTest
