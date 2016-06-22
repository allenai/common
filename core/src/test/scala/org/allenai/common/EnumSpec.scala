package org.allenai.common

import org.allenai.common.testkit.UnitSpec

import spray.json._

import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.nio.file.Files

class EnumSpec extends UnitSpec {

  "all" should "return all registerd Enum's" in {
    assert(FakeEnum.all.size === 3)
    assert(FakeEnum.all.toSet === Set(FakeEnum.Value1, FakeEnum.Value2, FakeEnum.Value3))
  }

  "withId" should "retrieve correct Enum" in {
    assert(FakeEnum.withId("Value1") === FakeEnum.Value1)
    assert(FakeEnum.withId("Value2") === FakeEnum.Value2)
    assert(FakeEnum.withId("Value3") === FakeEnum.Value3)
  }

  it should "throw NoSuchElementException" in {
    intercept[NoSuchElementException] {
      FakeEnum.withId("foo")
    }
  }

  "toString" should "act like builtin Enumeration" in {
    assert(FakeEnum.Value1.toString === "Value1")
  }

  "JSON serialization" should "work" in {
    FakeEnum.all foreach { enum =>
      val js = enum.toJson
      assert(js.convertTo[FakeEnum] eq enum)
    }
  }

  "Java serialization" should "work" in {
    FakeEnum.all foreach { enum =>
      val tmp = Files.createTempFile(enum.id, "dat")
      val tmpFile = tmp.toFile()
      tmpFile.deleteOnExit()
      Resource.using(new ObjectOutputStream(new FileOutputStream(tmpFile))) { os =>
        os.writeObject(enum)
      }
      val obj = Resource.using(new ObjectInputStream(new FileInputStream(tmpFile))) { is =>
        is.readObject()
      }
      obj should equal(enum)
      tmpFile.delete()
    }
  }
}

// Test enum. Must be defined outside of spec otherwise serialization tests will
// fail due to scalatest WordSpec not being serializable.
sealed abstract class FakeEnum extends Enum[FakeEnum]
object FakeEnum extends EnumCompanion[FakeEnum] {
  case object Value1 extends FakeEnum
  case object Value2 extends FakeEnum
  case object Value3 extends FakeEnum
  register(Value1, Value2, Value3)
}
