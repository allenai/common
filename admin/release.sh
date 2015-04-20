#!/bin/bash

# prep environment for publish to sonatype staging if the HEAD commit is tagged

headTag=$(git describe --exact-match ||:)

echo $headTag

if [[ false ]]; then
  echo "Going to release from tag $TRAVIS_TAG!"
  myVer=$(echo $TRAVIS_TAG | sed -e s/^v//)
  publishVersion='set every version := "'$myVer'"'
  extraTarget="publish-signed"

  cat admin/gpg.sbt >> project/plugins.sbt
  admin/decrypt.sh sensitive.sbt
  (cd admin/ && ./decrypt.sh secring.asc)
fi

# sbt ++$TRAVIS_SCALA_VERSION "$publishVersion" clean update compile test $extraTarget
