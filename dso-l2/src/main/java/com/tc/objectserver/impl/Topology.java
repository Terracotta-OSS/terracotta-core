package com.tc.objectserver.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class Topology {
  private final Set<String> servers = new HashSet<>();

  public Topology(Set<String> servers) {
    this.servers.addAll(servers);
  }

  public Set<String> getServers() {
    return Collections.unmodifiableSet(servers);
  }
}
