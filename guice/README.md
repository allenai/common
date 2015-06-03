This package contains a class, `ConfigModule`, that modules can extend to get bindings for Typesafe
Config entries.

See [the class scaladoc](src/main/scala/org/allenai/common/guice/ConfigModule.scala) for full
usage.

Example config:
```
stringValue = "foo"
someObject = {
  propNumber = 123
  propBool = true
}
```

Which will bind & let you inject:
```scala
class Injected @Inject() (
  @Named("stringValue") foo: String,
  @Named("someObject.propBool") boolValue: Boolean,
  @Named("someObject.propNumber") integralValue: Int,
  someOtherParameter: ScalaClass,
  @Named("someObject.propNumber") numbersCanBeDoubles: Double
)
```
