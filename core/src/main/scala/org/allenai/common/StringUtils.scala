package org.allenai.common

import org.apache.commons.lang3.{StringUtils => ApacheStringUtils}

object StringUtils {
  val whiteSpaceRegex = """\s+""".r
  val nonAsciiRegex = """[^\p{ASCII}]""".r
  val punctuationRegex = """[\p{Punct}]""".r
  // See http://stackoverflow.com/q/6198986
  val unprintableRegex = """[\p{Cc}\p{Cf}\p{Co}\p{Cn}]""".r
  val atLeastOneLowerCaseLetter = ".*[a-z]+.*".r

  val articles = Set("a", "an", "the")

  val simplePrepositions = Set(
    "a",
    "abaft",
    "aboard",
    "about",
    "above",
    "absent",
    "across",
    "afore",
    "after",
    "against",
    "along",
    "alongside",
    "amid",
    "amidst",
    "among",
    "amongst",
    "an",
    "apropos",
    "around",
    "as",
    "aside",
    "astride",
    "at",
    "athwart",
    "atop",
    "barring",
    "before",
    "behind",
    "below",
    "beneath",
    "beside",
    "besides",
    "between",
    "betwixt",
    "beyond",
    "but",
    "by",
    "circa",
    "concerning",
    "despite",
    "down",
    "during",
    "except",
    "excluding",
    "failing",
    "following",
    "for",
    "from",
    "given",
    "in",
    "including",
    "inside",
    "into",
    "lest",
    "like",
    "mid",
    "midst",
    "minus",
    "modulo",
    "near",
    "next",
    "notwithstanding",
    "of",
    "off",
    "on",
    "onto",
    "opposite",
    "out",
    "outside",
    "over",
    "pace",
    "past",
    "per",
    "plus",
    "pro",
    "qua",
    "regarding",
    "round",
    "sans",
    "save",
    "since",
    "than",
    "through",
    "thru",
    "throughout",
    "thruout",
    "till",
    "times",
    "to",
    "toward",
    "towards",
    "under",
    "underneath",
    "unlike",
    "until",
    "up",
    "upon",
    "versus",
    "vs.",
    "v.",
    "via",
    "vice",
    "with",
    "within",
    "without",
    "worth"
  )

  val coordinatingConjunction = Set("for", "and", "nor", "but", "or", "yet", "so")

  /** Maps weird unicode characters to ASCII equivalents
    * This list comes from http://goo.gl/qYCpw1
    */
  val fancyChar2simpleChar = Map(
    '\u00AB' -> "\"",
    '\u00AD' -> "-",
    '\u00B4' -> "'",
    '\u00BB' -> "\"",
    '\u00F7' -> "/",
    '\u01C0' -> "|",
    '\u01C3' -> "!",
    '\u02B9' -> "'",
    '\u02BA' -> "\"",
    '\u02BC' -> "'",
    '\u02C4' -> "^",
    '\u02C6' -> "^",
    '\u02C8' -> "'",
    '\u02CB' -> "`",
    '\u02CD' -> "_",
    '\u02DC' -> "~",
    '\u0300' -> "`",
    '\u0301' -> "'",
    '\u0302' -> "^",
    '\u0303' -> "~",
    '\u030B' -> "\"",
    '\u030E' -> "\"",
    '\u0331' -> "_",
    '\u0332' -> "_",
    '\u0338' -> "/",
    '\u0589' -> ":",
    '\u05C0' -> "|",
    '\u05C3' -> ":",
    '\u066A' -> "%",
    '\u066D' -> "*",
    '\u200B' -> " ",
    '\u2010' -> "-",
    '\u2011' -> "-",
    '\u2012' -> "-",
    '\u2013' -> "-",
    '\u2014' -> "-",
    '\u2015' -> "--",
    '\u2016' -> "||",
    '\u2017' -> "_",
    '\u2018' -> "'",
    '\u2019' -> "'",
    '\u201A' -> ",",
    '\u201B' -> "'",
    '\u201C' -> "\"",
    '\u201D' -> "\"",
    '\u201E' -> "\"",
    '\u201F' -> "\"",
    '\u2032' -> "'",
    '\u2033' -> "\"",
    '\u2034' -> "''",
    '\u2035' -> "`",
    '\u2036' -> "\"",
    '\u2037' -> "''",
    '\u2038' -> "^",
    '\u2039' -> "<",
    '\u203A' -> ">",
    '\u203D' -> "?",
    '\u2044' -> "/",
    '\u204E' -> "*",
    '\u2052' -> "%",
    '\u2053' -> "~",
    '\u2060' -> " ",
    '\u20E5' -> "\\",
    '\u2212' -> "-",
    '\u2215' -> "/",
    '\u2216' -> "\\",
    '\u2217' -> "*",
    '\u2223' -> "|",
    '\u2236' -> ":",
    '\u223C' -> "~",
    '\u2264' -> "<=",
    '\u2265' -> ">=",
    '\u2266' -> "<=",
    '\u2267' -> ">=",
    '\u2303' -> "^",
    '\u2329' -> "<",
    '\u232A' -> ">",
    '\u266F' -> "#",
    '\u2731' -> "*",
    '\u2758' -> "|",
    '\u2762' -> "!",
    '\u27E6' -> "[",
    '\u27E8' -> "<",
    '\u27E9' -> ">",
    '\u2983' -> "{",
    '\u2984' -> "}",
    '\u3003' -> "\"",
    '\u3008' -> "<",
    '\u3009' -> ">",
    '\u301B' -> "]",
    '\u301C' -> "~",
    '\u301D' -> "\"",
    '\u301E' -> "\"",
    '\uFEFF' -> " "
  )

  /** Extension methods for String. Meant to be mixed into an extension method implicit class,
    * which is why it extends Any. While we provide a StringImplicits implementation below that uses
    * this trait, we define this trait separately so that client projects can extend the extension
    * methods further (because you cannot extend an extension method class).
    *
    * To understand the quirky rules of extension method implicit classes,
    * see http://docs.scala-lang.org/overviews/core/value-classes.html#extension-methods
    *
    * A String extension method class should be defined like:
    * format: OFF
    * {{{
    * implicit class MyStringImplicits(val str: String) extends AnyVal with StringExtras {
    *   def anotherExtensionMethod = str.replaceAll("foo", "bar")
    * }}}
    * format: ON
    */
  trait StringExtras extends Any {

    /** value that is transformed by extension methods.
      * Must be declared as the constructor argument for the implementing  extension method class
      * (see trait scaladoc)
      */
    def str: String

    /** @return Trim white spaces, lower case, then strip the accents.
      */
    def normalize: String = whiteSpaceRegex.replaceAllIn(
      ApacheStringUtils.stripAccents(str.toLowerCase.trim),
      " "
    )

    def removeNonAlpha: String =
      str.map(c => if (Character.isAlphabetic(c.toInt)) c else ' ').mkString("")

    def splitOnWhitespace: Array[String] = whiteSpaceRegex.split(str)

    def removePunctuation: String = punctuationRegex.replaceAllIn(str, " ")

    def collapseWhitespace: String = whiteSpaceRegex.replaceAllIn(str, " ")

    def removeUnprintable: String = unprintableRegex.replaceAllIn(str, "")

    def replaceFancyUnicodeChars: String =
      fancyChar2simpleChar.foldLeft(str) {
        case (s, (unicodeChar, replacement)) =>
          s.replace(unicodeChar.toString, replacement)
      }

    /** @param filter Determine if a character is blacklisted and should be trimmed.
      * @return String with blacklisted chars trimmed from the right.
      */
    def trimRight(filter: Char => Boolean): String = {
      var i = str.size - 1
      while (i >= 0 && filter(str.charAt(i))) {
        i -= 1
      }
      str.substring(0, math.max(0, i + 1))
    }

    /** @return Trim non-letter chars from the beginning and end
      */
    def trimNonAlphabetic(): String =
      str.dropWhile(c => !Character.isAlphabetic(c)).trimRight(c => !Character.isAlphabetic(c))

    /** @param chars String containing the blacklist chars.
      * @return Trim characters from the right that belongs to a blacklist.
      */
    def trimChars(chars: String): String = str.trimRight(c => chars.contains(c))

    /** Properly capitalize a title.
      * http://titlecapitalization.com/#
      */
    def titleCase(): String = {
      val words = ApacheStringUtils.lowerCase(str).split("\\s+")

      for (i <- words.indices) {
        val word = words(i)

        // Capitalize the first and last words
        if (i == 0 || i == words.size - 1) {
          words.update(i, ApacheStringUtils.capitalize(word))
        } // Capitalize words that are not simple prepositions
        else if (!articles(word) &&
                 !simplePrepositions(word) &&
                 !coordinatingConjunction(word)) {
          words.update(i, ApacheStringUtils.capitalize(word))
        } // Otherwise, leave the word as lowercase
        else {
          words.update(i, word)
        }
      }

      words.mkString(" ")
    }

    def titleCaseIfAllCaps(): String = {
      if (atLeastOneLowerCaseLetter.findFirstIn(str).nonEmpty) {
        str
      } else {
        titleCase()
      }
    }

    def unescaped: String = {
      import org.apache.commons.lang3.StringEscapeUtils.{unescapeHtml4, unescapeXml}
      unescapeHtml4(unescapeXml(str))
    }
  }

  implicit class StringImplicits(val str: String) extends AnyVal with StringExtras
}
