`indexing`
=============

**Boss**: Roie

Builds an elasticsearch index on an existing ElasticSearch instance, using one of the configurations defined in the indexing.conf file in resources/org/allenai/ari/indexing.

To use this, you need to have a running instance of ElasticSearch (can be local or remote). As of this writing, the latest version is `1.7.2`. To install: Refer to http://joelabrahamsson.com/elasticsearch-101/ to get started, or use `brew install elasticsearch`
Once you have ElasticSearch, go to the `bin` directory and run: `./elasticsearch`

Configurations are of the form:

```
org.allenai.common.indexing.[NAME_OF_INDEX] 

      elasticSearch {
        clusterName: [CLUSTER_NAME]
        hostIp: "127.0.0.1"
        hostPort: 9300
        indexName:[NAME_OF_INDEX]
        indexType: "sentence"
    
        mapping = ${org.allenai.common.indexing.standardMapping}
      }
    
      buildIndexOptions {
        // If set to true will throw an exception if the index already exists
        // NOTE: if set to false, and a document already exists in the index, will create a duplicate document
        // This was an intentional design decision (otherwise elasticsearch would have to first issue an exists query).
        buildFromScratch = true
        // specifies where to dump serialized failed bulk index requests
        dumpFolder: "[PATH_TO_DUMP]"
      }
    
        // Template list of corpora
      corpora: [
        {
        // Specifies the format of the corpus, either "waterloo" or "datastore". Defaults to "datastore"
          corpusType: "waterloo"
          directory: "[PATH_TO_WATERLOO_FORMAT]"
        },
        {
          corpusType: "datastore"
          group: [DATASTORE_GROUP]
          directory: [DATASTORE_FOLDER_PATH]
          version: [VERSION_NO]
          file: [FILE_NAME]
        // specifies whether to use public or private datastore
          privacy: "public"
        },
      ]
    }
```
Configuration notes:

1. Do not change the mapping field unless you know what you are doing, otherwise existing solvers will not be able to query the index.
2. If using a waterloo format corpus, will attempt to split documents in folder based on `<SENT>...</SENT>` tags.
3. If using a datastore corpus: if a file is specified with no directory, will attempt to find the file in the default location. If a directory is specified with no file, will automatically walk the entire file tree and add index files that do not begin with "."
4. The `clusterName` of an instance is set in the `elasticsearch.yml` in `$ES_HOME/config`, which can be located by submitting the curl request: `curl -XGET 'localhost:9200/_nodes/settings'`. It is recommended you change this from `elasticsearch`, since elasticsearch has an autodiscovery feature that will automatically join your machine as a node to any clusters on the network with the same name. You will need to restart elasticsearch for configuration changes to take effect.

### Running instructions

To run, specify which configuration you which to use as the argument to BuildCorpusIndex in BuildCorpusIndexRunner.scala, and run BuildCorpusIndexRunner.scala. Running notes:

1. Make sure the ip address and port in the configuration correspond to those of the machine you which to build the index on.
2. If the buildFromScratch flag is set to true will not throw an exception if the index already exists, and add to the current index. However, if a document already exists in the index, will create a duplicate document. This was an intentional design decision (otherwise elasticsearch would have to first issue an exists query).
3. After executing all requests, will dump failed queries to a dump folder, and retry failed queries once.

Sample Command lines:

With Overrides to one or more index-building config parameters:

```
sbt "indexing/runMain org.allenai.common.indexing.BuildCorpusIndexRunner --index-name barrons --config-overrides-file /path/too/overrides.config"
```

Sample overrides:
```
{
  elasticSearch.clusterName: "solvercorpora"
}
```

Without Config Overrides:

```
sbt "indexing/runMain org.allenai.common.indexing.BuildCorpusIndexRunner --index-name barrons --config-overrides-file"
```

