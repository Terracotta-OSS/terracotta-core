/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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
