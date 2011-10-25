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

  public static final long              THOUSAND     = 1000;
  public static final long              MILLION      = THOUSAND * THOUSAND;
  public static final long              BILLION      = MILLION * THOUSAND;
  public static final long              TRILLION     = BILLION * THOUSAND;
  public static final long              QUADRILLION  = TRILLION * THOUSAND;

  /*
   * Long.MAX_VALUE == 2^63-1 == "9,223,372,036,854,775,807" == "9223.37Q"
   */

  @Override
  public final StringBuffer format(long number, StringBuffer result, FieldPosition fieldPosition) {
    StringBuffer sb;

    if (number < THOUSAND) {
      sb = stdFormatter.format(number, result, fieldPosition);
    } else if (number < MILLION) {
      sb = stdFormatter.format(number / (double) THOUSAND, result, fieldPosition);
      sb.append("K");
    } else if (number < BILLION) {
      sb = stdFormatter.format(number / (double) MILLION, result, fieldPosition);
      sb.append("M");
    } else if (number < TRILLION) {
      sb = stdFormatter.format(number / (double) BILLION, result, fieldPosition);
      sb.append("B");
    } else if (number < QUADRILLION) {
      sb = stdFormatter.format(number / (double) TRILLION, result, fieldPosition);
      sb.append("T");
    } else {
      sb = stdFormatter.format(number / (double) QUADRILLION, result, fieldPosition);
      sb.append("Q");
    }

    return sb;
  }

  @Override
  public final StringBuffer format(double number, StringBuffer result, FieldPosition fieldPosition) {
    return format((long) number, result, fieldPosition);
  }

  public static void main(String[] args) {
    ThinDecimalFormat tdf = new ThinDecimalFormat();
    long[] values = { 1L, 999L, 1023L, 130000400, 36854775807L, 372036854775807L, 9023372036854775807L, Long.MAX_VALUE };
    for (long value : values) {
      System.out.println(tdf.format(value));
    }
  }
}
