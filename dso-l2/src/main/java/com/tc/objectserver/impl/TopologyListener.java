package com.tc.objectserver.impl;

public interface TopologyListener {
  void nodeAdded(String host);

  void nodeRemoved(String host);
}
