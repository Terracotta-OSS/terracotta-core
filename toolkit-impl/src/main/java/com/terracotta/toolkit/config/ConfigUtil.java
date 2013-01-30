/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.config;

import java.util.Arrays;

public final class ConfigUtil {
  private static final int[] EMPTY_INT_ARRAY = new int[0];

  private ConfigUtil() {
    // private
  }

  public static int[] distributeInStripes(int configAttrInt, int numStripes) {
    if (numStripes == 0) { return EMPTY_INT_ARRAY; }
    int[] rv = new int[numStripes];
    int least = configAttrInt / numStripes;
    Arrays.fill(rv, least);
    int remainder = configAttrInt % numStripes;
    for (int i = 0; i < remainder; i++) {
      rv[i]++;
    }
    return rv;
  }

}
