/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.common;

import java.text.DecimalFormat;
import java.text.FieldPosition;

public class ThinDecimalFormat extends DecimalFormat {
  public static final ThinDecimalFormat INSTANCE = new ThinDecimalFormat();
  DecimalFormat stdFormatter = new DecimalFormat("0.##");

  public final StringBuffer format(long number, StringBuffer result, FieldPosition fieldPosition) {
    if (number < 1000) { return stdFormatter.format(number, result, fieldPosition); }
    
    StringBuffer sb;
    if (number < 1000000) {
      sb = stdFormatter.format(number/1000d, result, fieldPosition);
      sb.append("K");
      return sb;
    }
    if (number < 1000000000) {
      sb = stdFormatter.format(number/1000000d, result, fieldPosition);
      sb.append("M");
      return sb;
    }
    
    sb = stdFormatter.format(number/1000000000d, result, fieldPosition);
    sb.append("G");

    return sb;
  }

  public final StringBuffer format(double number, StringBuffer result, FieldPosition fieldPosition) {
    return format((long) number, result, fieldPosition);
  }
}
