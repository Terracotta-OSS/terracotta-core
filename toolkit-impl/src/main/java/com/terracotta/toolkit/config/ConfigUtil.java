/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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
