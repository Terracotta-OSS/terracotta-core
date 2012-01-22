/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util.stringification;

import com.tc.util.Assert;

/**
 * Contains methods for pretty-printing various things.
 */
public class PrettyPrintUtils {

  public static String pluralize(String base, int quantity) {
    if (quantity == 1) return base;
    else if (base.trim().toLowerCase().endsWith("s")) return base + "es";
    else return base + "s";
  }

  public static String quantity(String ofWhat, int howMany) {
    return "" + howMany + " " + pluralize(ofWhat, howMany);
  }

  public static String percentage(double value, int howManyDecimalDigits) {
    Assert.eval(howManyDecimalDigits >= 0);

    value *= 100.0;

    int integral = howManyDecimalDigits > 0 ? (int) value : (int) Math.round(value);
    int fraction = (int) Math.round(Math.abs(value - integral) * 100.0);

    String integralPart = Integer.toString(integral);
    String fractionPart = Integer.toString(fraction);
    while (fractionPart.length() < howManyDecimalDigits)
      fractionPart = "0" + fractionPart;

    if (howManyDecimalDigits == 0) return integralPart + "%";
    else return integralPart + "." + fractionPart + "%";
  }

}
