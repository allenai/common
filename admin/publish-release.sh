#!/bin/bash

# Publish to BinTray if the HEAD commit is tagged with a version number.

headTag=$(git describe --exact-match ||:)
echo "Current tag is: $headTag"

if [[ headTag =~ ^v[0-9]+\.[0-9]+\.[0-9]+(-[A-Za-z0-9-]+)? ]]; then
  echo "Going to release from tag $headTag."
  version=$(echo $headTag | sed -e s/^v//)

  sbt publish
fi
