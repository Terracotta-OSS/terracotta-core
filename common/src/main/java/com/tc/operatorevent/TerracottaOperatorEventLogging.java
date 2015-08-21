/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.operatorevent;

import com.tc.util.Assert;

public class TerracottaOperatorEventLogging {
  private static volatile NodeNameProvider nodeNameProvider;

  public static TerracottaOperatorEventLogger getEventLogger() {
    if (nodeNameProvider == null) {
      nodeNameProvider = NodeNameProvider.DEFAULT_NODE_NAME_PROVIDER;
    }
    return TerracottaOperatorEventLoggerHolder.instance;
  }

  public static void setNodeNameProvider(NodeNameProvider nameProvider) {
    if (nodeNameProvider == null) {
      nodeNameProvider = nameProvider;
    }
    Assert.assertNotNull(nodeNameProvider);
  }

  private static class TerracottaOperatorEventLoggerHolder {
    static final TerracottaOperatorEventLogger instance = new TerracottaOperatorEventLogger(nodeNameProvider);
  }
}
