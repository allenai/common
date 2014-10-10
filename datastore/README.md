# Datastore API

The Datastore gives us a way to store and retrieve immutable files and directories. It follows the Nexus model of publishing software, but for data.

The datastore stores *items*, which can be files or directories. Every item belongs to a *group*, and has a *name* and a *version*. The *group* is just a string, but per convention it should be of the form `org.allenai.corpora`. The *name* is also just a string, but it should be CamelCase, for example `WebSentences`. The *version* is just an integer.

When you request an item from the datastore, it will download the item from S3 and put it into the cache, which is a file or directory on the local file system. The path it returns is a path to that file or directory. If it's already there, it skips the download and simply returns the path.

Datastores have names. Currently, we have the `public` datastore, and the `private` one. `public` is world-accessible, while `private` is limited to AI2. This is not a feature of the datastore, just a result of the bucket configuration in S3. The default datastore is `public`.

[The Datastore has a command line tool. Click to go to its documentation.](../datastore-cli/README.md)

## Getting started

### Reading from the datastore

To get a file from the default datastore, simply call this:
```scala
// Get version 4 of GreedyParserModel.json in the
// group org.allenai.parsers.poly-parser
val path: java.nio.file.Path =
  Datastore.filePath(
    "org.allenai.parsers.poly-parser",
    "GreedyParserModel.json",
    4)
```

To get a directory, call this:
```scala
// Get version 1 of the WordNet directory in the
// group org.allenai.otter
val path: java.nio.file.Path =
  Datastore.directoryPath(
    "org.allenai.otter",
    "WordNet",
    1)
```

You can do anything with the resulting path except write to it.

To access a non-default datastore, for example the `private` one, call it like this:
```scala
val path: java.nio.file.Path =
  Datastore("private").directoryPath(
    "org.allenai.otter",
    "WordNet",
    1)
```

There is no way to automatically get the latest version from the datastore. This is by design. If you depend on the "latest" version of an item, your results are not reproducible, because someone might publish a new version and thus change what your code does.

### Publishing to the datastore

There are two main ways to write to the datastore, one for files, and one for directories:
```scala
// publish BigModel.json under the name
// "GreedyParserModel.json", version 4
Datastore.publishFile(
  "BigModel.json",
  "org.allenai.parsers.poly-parser",
  "GreedyParserModel.json",
  4,
  false)

// publish the wordnet directory under the
// name "WordNet", version 1, and do so privately
Datastore("private").publishDirectory(
  "wordnet",
  "org.allenai.otter",
  "WordNet",
  1,
  false)
```

## Authentication

The datastore client needs to be authenticated with AWS. This happens using Amazon's default methods. In detail, this is what that means:

### On your Mac

Create a file in `~/.aws/credentials`, with the following content:

```
[default]
aws_access_key_id = <MYACCESSKEY>
aws_secret_access_key = <mysecretaccesskey>
```

Please replace `<MYACCESSKEY>` and `<mysecretaccesskey>` as appropriate. You can get these credentials in the [AWS Console, under IAM/Users](https://console.aws.amazon.com/iam/home?region=us-west-2#users). Click on your username, and then "Manage Credentials". You should be able to add a key pair there.

### In EC2

In EC2, Amazon promises that the credentials will be fetched from the Amazon EC2 Metadata Service, which should be completely transparent and need no intervention by an admin. If that doesn't work, we can still use the approach that works for the Mac.

### Other options

Since the datastore is just delegating authentication to the Amazon SDK, [all the possibilities from the SDK work](http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/s3/AmazonS3Client.html#AmazonS3Client()).

You can also go completely manual and create the datastore with a access key and secret key pair. To do this, create a datastore like this:
```scala
val datastore = Datastore("<myaccesskey>", "<mysecretkey>")
val privateDatastore = Datastore("private", "<myaccesskey>", "<mysecretkey>")
```


## Cache location

The cache lives in the directory pointed to by the system property `java.io.tempdir`. On Linux, this is `/tmp`. On Mac, this is somewhere in `/var/folders`. Losing the cache is not harmful, but it means that everything has to be downloaded again.

## Concurrency

The datastore is completely thread-safe. Similarly, two processes (not threads) requesting the same item at the same time will not fall over each other, and will not download the same file twice.

To achieve this, it assumes that temporary files are created on the same file system where the cache lives. This is the case in virtuall all instances. However, if that is not the case, due to a change in cache location, or by virtue of a really quirky setup, it will no longer be safe.
