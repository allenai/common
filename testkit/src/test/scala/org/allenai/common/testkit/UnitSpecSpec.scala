package org.allenai.common.testkit

class UnitSpecSpec extends UnitSpec {

  // A spec is structured as:
  // [subject] should [expected behavior] in { [block containing assertions] }
  "UnitSpec" should "test assertions" in {
    // ScalaTest provides a special triple equals operator which can produce detailed messages
    // when the assertion fails (such as tell you the value received vs. value expected)
    assert("foo" === "foo")
  }

  // To continue making assertions against the same subject (in this case UnitSpec),
  // you can use the pronoun 'it'
  it should "test for exceptions" in {
    intercept[Exception] {
      throw new Exception("foo")
    }
  }

  // You can have more than one subjects tested in a given spec
  // Here, the next subject is the awesome Foo
  "Foo" should "do foo" in {
    // You can flag a test as pending with (you guessed it) pending
    pending
  }

  // For many more examples, see http://www.scalatest.org/quick_start

}
