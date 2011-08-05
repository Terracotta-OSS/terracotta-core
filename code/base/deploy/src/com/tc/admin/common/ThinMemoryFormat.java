/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.common;

import java.text.DecimalFormat;
import java.text.FieldPosition;

public class ThinMemoryFormat extends DecimalFormat {
  public static final ThinMemoryFormat INSTANCE     = new ThinMemoryFormat();

  private static final DecimalFormat   stdFormatter = new DecimalFormat("0.##");

  public static final long             KBYTE        = 1024;
  public static final long             MBYTE        = KBYTE * KBYTE;
  public static final long             GBYTE        = MBYTE * KBYTE;
  public static final long             TBYTE        = GBYTE * KBYTE;

  @Override
  public final StringBuffer format(long number, StringBuffer result, FieldPosition fieldPosition) {
    StringBuffer sb;

    if (number < KBYTE) {
      sb = stdFormatter.format(number, result, fieldPosition);
      sb.append("B");
    } else if (number < MBYTE && (number / KBYTE) < 1000) {
      sb = stdFormatter.format(number / (double) KBYTE, result, fieldPosition);
      sb.append("KB");
    } else if (number < GBYTE && (number / MBYTE) < 1000) {
      sb = stdFormatter.format(number / (double) MBYTE, result, fieldPosition);
      sb.append("MB");
    } else if (number < TBYTE && (number / GBYTE) < 1000) {
      sb = stdFormatter.format(number / (double) GBYTE, result, fieldPosition);
      sb.append("GB");
    } else {
      sb = stdFormatter.format(number / (double) TBYTE, result, fieldPosition);
      sb.append("TB");
    }

    return sb;
  }

  @Override
  public final StringBuffer format(double number, StringBuffer result, FieldPosition fieldPosition) {
    return format((long) number, result, fieldPosition);
  }
}
