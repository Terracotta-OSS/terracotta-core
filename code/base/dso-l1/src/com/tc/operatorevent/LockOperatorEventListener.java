/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.operatorevent;

import com.tc.logging.TerracottaOperatorEventLogger;
import com.tc.logging.TerracottaOperatorEventLogging;

public class LockOperatorEventListener implements LockEventListener {
  
  TerracottaOperatorEventLogger operatorEventLogger = TerracottaOperatorEventLogging.getEventLogger();

  public void fireLockGCEvent(int gcCount) {
    operatorEventLogger.fireOperatorEvent(TerracottaOperatorEventFactory.createLockGCEvent(new Object[] { gcCount }));
  }

}
