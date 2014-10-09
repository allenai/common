# Datastore CLI

This is the command line interface to the datastore. [For the documentation of the API look here](../datastore/README.md).

The datastore CLI can do everything the API can do, but from the command line. That is, publish items, download items, list the contents of a datastore, and some auxiliary things.

## Building and installation

When you're in the root of the `common` project, run `sbt datastore/assembly`. After a few seconds, it will produce an executable jar file named `DatastoreCLI.jar`, and print the path where it put it. To run the CLI, you have to refer to this jar file. If you want to run the CLI a lot, it might make sense to copy it somewhere safe, and set up an alias.

## Some common actions

The CLI has five commands, `upload`, `download`, `list`, `url`, and `wipeCache`. Here are some examples:

### Upload

```
# Upload a BigModel.json to GreedyParserModel.poly.json
# in the group org.allenai.parsers.poly-parser
# with version 5
> java -jar DatastoreCLI.jar upload -p BigModel.json -n GreedyParserModel.poly.json -g org.allenai.parsers.poly-parser -v 5

# Upload the same file to the private datastore
> java -jar DatastoreCLI.jar upload -p BigModel.json -n GreedyParserModel.poly.json -g org.allenai.parsers.poly-parser -v 5 -d private
```

### Download

```
# Download the file from the upload example
> java -jar DatastoreCLI.jar download -n GreedyParserModel.poly.json -g org.allenai.parsers.poly-parser -v 5
/var/folders/m2/pg9dfmz941zddvhm99lkk3n40000gn/T/ai2-datastore-cache/public/org.allenai.parsers.poly-parser/GreedyParserModel.poly-v5.json
```

The download command does not copy the file into your current directory or anything like that. It downloads the file into the cache, and then prints the location of the file in the cache. Note that the filename is not completely preserved. The datastore preserves the extension, but it does not preserve the whole name.

### List

```
# List all groups in the private datastore
> java -jar DatastoreCLI.jar ls -d private
org.allenai.parsers.poly-parser
org.allenai.otter
cc.factorie.apps.nlp
org.allenai.corport

# List all items in the org.allenai.corpora group in the private datastore
> java -jar DatastoreCli.jar ls -d private -g org.allenai.corpora
Barrons/	1
CK12-Biology/	1
WebSentences/	1
Holt-LifeScience/	1
McGrawHill-Science/	1
```

When listing the contents of a group, the result is a table. The first column contains the name of the item. The second column contains the version. All versions are shown at once. Directories are indicated by a name that ends in a slash. In this example, all items in the group are directories.
