package org.allenai.common.cache

class QueryCaches(
    stringQueryCache: JsonQueryCache[String],
    intQueryCache: JsonQueryCache[Int],
    seqStringQueryCache: JsonQueryCache[Seq[String]],
    fooQueryCache: JsonQueryCache[Foo]
) {
  val stringKey = "stringKey"
  val stringValue = "stringValue"

  val intKey = "intKey"
  val intValue = 32

  val seqStringKey = "seqStringKey"
  val seqStringValue = Seq("a string", "a second string", "third time's the charm")

  val fooKey = "fooKey"
  val fooValue = new Foo("stringerino", 42)

  def getAll(): Seq[Option[Any]] = Seq(
    stringQueryCache.get(stringKey),
    intQueryCache.get(intKey),
    seqStringQueryCache.get(seqStringKey),
    fooQueryCache.get(fooKey)
  )

  def putAll(): Unit = {
    stringQueryCache.put(stringKey, stringValue)
    intQueryCache.put(intKey, intValue)
    seqStringQueryCache.put(seqStringKey, seqStringValue)
    fooQueryCache.put(fooKey, fooValue)
  }

  def delAll(): Unit = {
    stringQueryCache.del(stringKey)
    intQueryCache.del(intKey)
    seqStringQueryCache.del(seqStringKey)
    fooQueryCache.del(fooKey)
  }

  def allThereAndEq(): Boolean = {
    stringQueryCache.get(stringKey).exists(_.equals(stringValue)) &&
    intQueryCache.get(intKey).exists(_.equals(intValue)) &&
    seqStringQueryCache.get(seqStringKey).exists(_.equals(seqStringValue)) &&
    fooQueryCache.get(fooKey).exists(_.equals(fooValue))
  }
}
