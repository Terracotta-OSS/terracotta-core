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
package com.tc.object.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class TransparencyClassSpecUtil {

  private static final Map    anomalies     = Collections.synchronizedMap(new HashMap());

  private static final String IGNORE_CHECKS = "IGNORE_CHECKS";

  static {
    anomalies.put("org.apache.commons.collections.FastHashMap", IGNORE_CHECKS);
  }

  private TransparencyClassSpecUtil() {
    super();
  }

  public static boolean ignoreChecks(String className) {
    return IGNORE_CHECKS.equals(anomalies.get(className));
  }

}
