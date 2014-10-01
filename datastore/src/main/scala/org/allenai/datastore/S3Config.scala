package org.allenai.datastore

import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.AmazonS3Client

case class S3Config(
    service: AmazonS3Client = new AmazonS3Client(),
    bucket: String = S3Config.defaultBucketName) {
  def this(accessKey: String, secretAccessKey: String, bucket: String) =
    this(new AmazonS3Client(new BasicAWSCredentials(accessKey, secretAccessKey)), bucket)

  def this(bucketName: String) = this(new AmazonS3Client(), bucketName)
}

object S3Config {
  val defaultBucketName = "ai2-datastore-public"
}
