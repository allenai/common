package org.allenai.vault

import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.AmazonS3Client

case class S3Config(service: AmazonS3Client, bucket: String)

object S3Config {
  val defaultBucketName = "ai2-vault"

  def apply(accessKey: String, secretAccessKey: String, bucket: String): S3Config =
    S3Config(new AmazonS3Client(new BasicAWSCredentials(accessKey, secretAccessKey)), bucket)

  def apply(bucket: String): S3Config = S3Config(new AmazonS3Client(), bucket)

  def apply() = apply(defaultBucketName)
}
