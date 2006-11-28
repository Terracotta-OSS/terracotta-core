package com.tctest.perf.collections;

import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;

public class ArrayListPerfTestApp extends CollectionsPerfTestAppBase {

  public ArrayListPerfTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    setCollection(new CollectionType.ArrayListCollection());
  }
}
