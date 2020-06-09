package org.allenai.common.guice

import com.google.inject.{ Guice, Inject }
import com.google.inject.name.Named
import com.typesafe.config.{ Config, ConfigFactory }
import org.allenai.common.testkit.UnitSpec

case class CaseClass(a: String)
// Test class, defined in a way that's injectable by Guice (outside of a wrapping class).
case class AnnotatedClass @Inject()(
  @Named("fooString") foo: String,
  // This string has a default value in the module.conf file.
  @Named("hasDefault") hasDefault: String,
  unannotated: Set[String],
  @Named("boolbool") boolean: Boolean,
  @Named("barNum") bar: Int,
  @Named("barNum") barLong: Long,
  @Named("barNum") barDouble: Double,
  @Named("unsupported") unsupported: CaseClass
)

case class OptionalParamClass @Inject()(
  @Named("presentString") present: String,
  @Named("presentString") presentOption: Option[String],
  @Named("missingString") missingOption: Option[String]
)

// Test class with nested Config objects.
case class NestedConfig @Inject()(
  @Named("root") root: Config,
  @Named("root.nested") nested: Config,
  @Named("nested") nestedNone: Option[Config],
  @Named("root.string") rootString: String,
  @Named("root.nested.string") nestedString: String
)

// Test class, using namespaced values.
case class PrefixClass @Inject()(
  @Named("prefix.fooString") foo: String,
  // This string has a default value in the module.conf file.
  @Named("prefix.hasDefault") hasDefault: String,
  @Named("prefix.boolbool") boolean: Boolean,
  @Named("prefix.nested.bool") nestedBool: Boolean,
  // This doesn't begin with the right prefix, so it shouldn't get a binding.
  @Named("ignored_no_prefix") bar: Int
)

// Test class with dotted keys.
case class DottedKeys @Inject()(
  @Named("\"i.have\".dots") dots: String,
  @Named("\"i.have.more.dots\".bar") bar: Int
)

// Test class with Seq values.
case class SeqValues @Inject()(
  @Named("seq.ofConfig") configs: Seq[Config],
  @Named("seq.ofString") strings: Seq[String],
  @Named("seq.ofBool") booleans: Seq[Boolean],
  @Named("seq.ofDouble") doubles: Seq[Double]
)

class ConfigModuleSpec extends UnitSpec {
  "bindConfig" should "bind config values to appropriate @Named bindings" in {
    // Config with an entry for all of the bindable values except the one with a default.
    val testConfig = ConfigFactory.parseString("""
      hasDefault = "default"
      fooString = "Foo"
      barNum = 42
      boolbool = true
    """)
    val testModule = new ConfigModule(testConfig) {
      override def configureWithConfig(c: Config): Unit = {
        // Manually bind things missing from the config.
        bind[Set[String]].toInstance(Set("unannotated"))
        bind[CaseClass].annotatedWithName("unsupported").toInstance(CaseClass("instance"))
      }
    }

    val injector = Guice.createInjector(testModule)

    val annotatedClassInstance = injector.getInstance(classOf[AnnotatedClass])

    // Verify bindings. They should match the defaults plus the provided config plus the
    // manually-bound values.
    annotatedClassInstance should be(
      AnnotatedClass("Foo", "default", Set("unannotated"), true, 42, 42, 42, CaseClass("instance"))
    )
  }

  it should "allow setting a default config name" in {
    // Config with an entry for all of the bindable values except the one with a default.
    val testConfig = ConfigFactory.parseString("""
      fooString = "Foo"
      barNum = 42
      boolbool = true
    """)
    val testModule = new ConfigModule(testConfig) {
      override def configName: Option[String] = Some("test_default.conf")

      override def configureWithConfig(c: Config): Unit = {
        // Manually bind things missing from the config.
        bind[Set[String]].toInstance(Set("unannotated"))
        bind[CaseClass].annotatedWithName("unsupported").toInstance(CaseClass("instance"))
      }
    }

    val injector = Guice.createInjector(testModule)

    val annotatedClassInstance = injector.getInstance(classOf[AnnotatedClass])

    // Verify bindings. They should match the config plus the manually-bound values.
    annotatedClassInstance should be(
      AnnotatedClass(
        "Foo",
        "New default!",
        Set("unannotated"),
        true,
        42,
        42,
        42,
        CaseClass("instance")
      )
    )
  }

  it should "allow overriding default configs" in {
    // Config with an entry for all of the bindable values.
    val testConfig = ConfigFactory.parseString("""
      hasDefault = "new val"
      fooString = "Foo"
      barNum = 42
      boolbool = true
      """)
    val testModule = new ConfigModule(testConfig) {
      override def configName: Option[String] = Some("test_default.conf")

      override def configureWithConfig(c: Config): Unit = {
        // Manually bind things missing from the config.
        bind[Set[String]].toInstance(Set("unannotated"))
        bind[CaseClass].annotatedWithName("unsupported").toInstance(CaseClass("instance"))
      }
    }

    val injector = Guice.createInjector(testModule)

    val annotatedClassInstance = injector.getInstance(classOf[AnnotatedClass])

    // Verify bindings. They should match the config plus the manually-bound values.
    annotatedClassInstance should be(
      AnnotatedClass("Foo", "new val", Set("unannotated"), true, 42, 42, 42, CaseClass("instance"))
    )
  }

  it should "allow a default namespace" in {
    // Config with non-default entries, plus a bonus entry that shouldn't be bound.
    val testConfig = ConfigFactory.parseString("""
      hasDefault = "default"
      fooString = "Foo"
      boolbool = true
      nested.bool = true
      ignored_no_prefix = "Should be ignored"
      """)
    val testModule = new ConfigModule(testConfig) {
      override def bindingPrefix: Option[String] = Some("prefix")
      override def configureWithConfig(c: Config): Unit = {
        bind[Int].annotatedWithName("ignored_no_prefix").toInstance(33)
      }
    }

    val injector = Guice.createInjector(testModule)

    val instance = injector.getInstance(classOf[PrefixClass])

    // Verify bindings.
    instance should be(PrefixClass("Foo", "default", true, true, 33))
  }

  it should "bind Option values" in {
    // Config with one present value and one missing value.
    val testConfig = ConfigFactory.parseString("""
      presentString = "here"
      // missingString = "missing"
    """)
    val testModule = new ConfigModule(testConfig)

    val injector = Guice.createInjector(testModule)

    val instance = injector.getInstance(classOf[OptionalParamClass])

    // Verify bindings.
    instance should be(OptionalParamClass("here", Some("here"), None))
  }

  it should "bind Config objects for intermediary configs" in {
    // Config with nested values.
    val testConfig = ConfigFactory.parseString("""
      root = {
        string = "root string"
        nested = {
          string = "nested string"
        }
      }
      """)

    val testModule = new ConfigModule(testConfig)

    val injector = Guice.createInjector(testModule)

    val instance = injector.getInstance(classOf[NestedConfig])

    // Verify bindings.
    instance.nestedNone should be(None)
    instance.rootString should be("root string")
    instance.nestedString should be("nested string")

    instance.root.getString("nested.string") should be("nested string")
    instance.root.getString("string") should be("root string")
    instance.nested.getString("string") should be("nested string")
  }

  it should "handle keys containing dots" in {
    val testConfig = ConfigFactory.parseString("""
      "i.have".dots = "foo"
      "i.have.more.dots" = {
        bar = 123
      }
      """)
    val testModule = new ConfigModule(testConfig)

    val injector = Guice.createInjector(testModule)

    injector.getInstance(classOf[DottedKeys])
  }

  it should "handle sequences" in {
    val testConfig = ConfigFactory.parseString("""
      seq.ofConfig = [ {a: "a"}, {b: "b"} ]
      seq.ofString = [ "foo", "bar" ]
      seq.ofBool = [ true, false, true ]
      seq.ofDouble = [ 1, 2 ]
      """)
    val testModule = new ConfigModule(testConfig)

    val injector = Guice.createInjector(testModule)

    val instance = injector.getInstance(classOf[SeqValues])
    instance.strings shouldBe Seq("foo", "bar")
    instance.booleans shouldBe Seq(true, false, true)
    instance.doubles shouldBe Seq(1.0, 2.0)
  }
}
