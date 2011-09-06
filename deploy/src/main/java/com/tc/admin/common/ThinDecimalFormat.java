/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.common;

import java.text.DecimalFormat;
import java.text.FieldPosition;

public class ThinDecimalFormat extends DecimalFormat {
  public static final ThinDecimalFormat INSTANCE     = new ThinDecimalFormat();

  private static final DecimalFormat    stdFormatter = new DecimalFormat("0.##");

  public static final long              KILO        = 1000;
  public static final long              MEGA         = KILO * KILO;
  public static final long              GIGA         = MEGA * KILO;
  public static final long              TERA         = GIGA * KILO;

  @Override
  public final StringBuffer format(long number, StringBuffer result, FieldPosition fieldPosition) {
    StringBuffer sb;

    if (number < KILO) {
      sb = stdFormatter.format(number, result, fieldPosition);
    } else if (number < MEGA) {
      sb = stdFormatter.format(number / (double) KILO, result, fieldPosition);
      sb.append("K");
    } else if (number < GIGA) {
      sb = stdFormatter.format(number / (double) MEGA, result, fieldPosition);
      sb.append("M");
    } else if (number < TERA) {
      sb = stdFormatter.format(number / (double) GIGA, result, fieldPosition);
      sb.append("G");
    } else {
      sb = stdFormatter.format(number / (double) TERA, result, fieldPosition);
      sb.append("T");
    }

    return sb;
  }

  @Override
  public final StringBuffer format(double number, StringBuffer result, FieldPosition fieldPosition) {
    return format((long) number, result, fieldPosition);
  }
}
