package com.tctest.perf.collections;

import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;

public class VectorPerfTestApp extends CollectionsPerfTestAppBase {
  public VectorPerfTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    setCollection(new CollectionType.VectorCollection());
  }
}
