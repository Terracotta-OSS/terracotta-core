/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.longrunning;

import com.tc.simulator.app.ApplicationConfig;

public interface LongrunningGCTestAppConfig extends ApplicationConfig {
  public long getLoopSleepTime();
}