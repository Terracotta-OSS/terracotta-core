/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tctest.longrunning;

import com.tc.simulator.app.ApplicationConfig;

public interface LongrunningGCTestAppConfig extends ApplicationConfig {
  public long getLoopSleepTime();
}