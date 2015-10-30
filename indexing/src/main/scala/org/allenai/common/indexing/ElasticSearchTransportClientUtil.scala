package org.allenai.common.indexing

import org.allenai.common.Logging

import com.typesafe.config.Config
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.settings.ImmutableSettings
import org.elasticsearch.common.transport.InetSocketTransportAddress

/** Utility object that takes config parameters from application config file and constructs a
  * transport client to talk to ElasticSearch.
  */
object ElasticSearchTransportClientUtil extends Logging {

  /** Build the Transport client from the config.
    * @param esConfig config with the address/port/name of the target cluster
    * @param sniffMode flag that specifies whether to auto-detects other nodes of the cluster on
    *                  connection fault. See: "https://www.elastic.co/guide/en/elasticsearch/client/
    *                  java-api/current/transport-client.html"
    * @return the constructed TransportClient
    */
  def ConstructTransportClientFromESconfig(
    esConfig: Config,
    sniffMode: Boolean = false
  ): TransportClient = {
    val settings = ImmutableSettings.builder()
      .put("cluster.name", esConfig.getString("clusterName"))
      .put("client.transport.sniff", sniffMode)
      .put("sniffOnConnectionFault", sniffMode)
      .build()
    val address = new InetSocketTransportAddress(
      esConfig.getString("hostIp"),
      esConfig.getInt("hostPort")
    )

    logger.debug(s"Created Elastic Search Client in cluster ${esConfig.getString("clusterName")}")
    new TransportClient(settings).addTransportAddresses(address)
  }
}
