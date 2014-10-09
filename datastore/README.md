# Datastore API

The Datastore gives us a way to store and retrieve immutable files and directories. It follows the Nexus model of publishing software, but for data.

The datastore stores *items*, which can be files or directories. Every item belongs to a *group*, and has a *name* and a *version*. The *group* is just a string, but per convention it should be of the form `org.allenai.corpora`. The *name* is also just a string, but it should be CamelCase, for example `WebSentences`. The *version* is just an integer.

When you request an item from the datastore, it will download the item from S3 and put it into the cache, which is a file or directory on the local file system. The path it returns is a path to that file or directory. If it's already there, it skips the download and simply returns the path.

Datastores have names. Currently, we have the `public` datastore, and the `private` one. `public` is world-accessible, while `private` is limited to AI2. This is not a feature of the datastore, just a result of the bucket configuration in S3. The default datastore is `public`.

## Getting started

To get a file from the default datastore, simply call this:
```scala
val path = Datastore.filePath("org.allenai.store", "ExampleFile", 1)
```

To get a directory, call this:
```scala
val path = Datastore.directoryPath("org.allenai.store", "ExampleDirectory", 1)
```

You can do anything with the resulting path except write to it.

To access a non-default datastore, for example the `private` one, call it like this:
```scala
val path = Datastore("private").filePath("org.allenai.store", "ExampleFile", 1)
```

## Authentication

## Cache location

The cache lives in the directory pointed to by the system property `java.io.tempdir`. On Linux, this is `/tmp`. On Mac, this is somewhere in `/var/folders`. Losing the cache is not harmful, but it means that everything has to be downloaded again.

## Concurrency

The datastore is completely thread-safe. Similarly, two processes (not threads) requesting the same item at the same time will not fall over each other, and will not download the same file twice.

To achieve this, it assumes that temporary files are created on the same file system where the cache lives. This is the case in virtuall all instances. However, if that is not the case, due to a change in cache location, or by virtue of a really quirky setup, it will no longer be safe.
