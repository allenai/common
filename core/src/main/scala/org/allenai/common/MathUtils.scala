package org.allenai.common

/** Various convenient utilities for math operations. */
object MathUtils {

  /** Round a Double to k decimal digits. */
  def round(d: Double, precision: Int): Double = {
    BigDecimal(d).setScale(precision, BigDecimal.RoundingMode.HALF_UP).toDouble
  }
}
