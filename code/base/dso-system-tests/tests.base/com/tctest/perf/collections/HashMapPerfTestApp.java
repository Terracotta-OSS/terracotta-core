/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tctest.perf.collections;

import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;

public class HashMapPerfTestApp extends CollectionsPerfTestAppBase {

  public HashMapPerfTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    this.setCollection(new CollectionType.HashMapCollection());
  }

}
