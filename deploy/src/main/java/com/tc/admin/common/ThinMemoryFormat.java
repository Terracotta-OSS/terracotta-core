/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.common;

import org.apache.commons.lang.StringUtils;

import java.text.DecimalFormat;
import java.text.FieldPosition;

public class ThinMemoryFormat extends DecimalFormat {
  public static final ThinMemoryFormat INSTANCE                   = new ThinMemoryFormat();

  private static final DecimalFormat   DEFAULT_STANDARD_FORMATTER = new DecimalFormat("0.##");

  private final DecimalFormat          format;

  public static final long             BYTE                       = 1;
  public static final long             KBYTE                      = 1024 * BYTE;
  public static final long             MBYTE                      = KBYTE * KBYTE;
  public static final long             GBYTE                      = MBYTE * KBYTE;
  public static final long             TBYTE                      = GBYTE * KBYTE;

  public ThinMemoryFormat() {
    this((DecimalFormat) null);
  }

  public ThinMemoryFormat(String formatPattern) {
    this(StringUtils.isEmpty(formatPattern) ? null : new DecimalFormat(formatPattern));
  }

  public ThinMemoryFormat(DecimalFormat format) {
    super();
    this.format = format;
  }

  @Override
  public final StringBuffer format(long number, StringBuffer result, FieldPosition fieldPosition) {
    StringBuffer sb;
    DecimalFormat df = getFormat();

    if (number < KBYTE) {
      sb = df.format(number, result, fieldPosition);
      sb.append("B");
    } else if (number < MBYTE && (number / KBYTE) < 1000) {
      sb = df.format(number / (double) KBYTE, result, fieldPosition);
      sb.append("KB");
    } else if (number < GBYTE && (number / MBYTE) < 1000) {
      sb = df.format(number / (double) MBYTE, result, fieldPosition);
      sb.append("MB");
    } else if (number < TBYTE && (number / GBYTE) < 1000) {
      sb = df.format(number / (double) GBYTE, result, fieldPosition);
      sb.append("GB");
    } else {
      sb = df.format(number / (double) TBYTE, result, fieldPosition);
      sb.append("TB");
    }

    return sb;
  }

  @Override
  public final StringBuffer format(double number, StringBuffer result, FieldPosition fieldPosition) {
    return format((long) number, result, fieldPosition);
  }

  private DecimalFormat getFormat() {
    return format != null ? format : DEFAULT_STANDARD_FORMATTER;
  }
}
