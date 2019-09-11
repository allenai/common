package org.allenai.common

import scala.math.BigDecimal.RoundingMode

/** Various convenient utilities for math operations. */
object MathUtils {

  /** Round a Double to k decimal digits; by default, 0.5 rounds upwards. */
  def round(
      double: Double,
      precision: Int,
      roundingMode: RoundingMode.Value = RoundingMode.HALF_UP
  ): Double = {
    BigDecimal(double).setScale(precision, roundingMode).toDouble
  }
}
