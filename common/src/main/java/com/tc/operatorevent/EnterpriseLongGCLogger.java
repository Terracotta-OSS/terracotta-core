/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.operatorevent;

import com.tc.runtime.logging.LongGCLogger;

public class EnterpriseLongGCLogger extends LongGCLogger {
  private final TerracottaOperatorEventLogger operatorEventLogger = TerracottaOperatorEventLogging.getEventLogger();

  public EnterpriseLongGCLogger(long gcTimeOut) {
    super(gcTimeOut);
  }

  @Override
  protected void fireLongGCEvent(TerracottaOperatorEvent tcEvent) {
    this.operatorEventLogger.fireOperatorEvent(tcEvent);
  }
}
