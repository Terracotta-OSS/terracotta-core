/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;

public class EhcacheGlobalEviction130TestApp extends EhcacheGlobalEvictionTestApp {
  private final static int NUM_OF_L1 = 2;

  public EhcacheGlobalEviction130TestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
  }
}
