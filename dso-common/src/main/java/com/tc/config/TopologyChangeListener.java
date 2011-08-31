/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config;

public interface TopologyChangeListener {
  void topologyChanged(ReloadConfigChangeContext context);
}
