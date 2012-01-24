/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema;

public interface ActiveServerGroupsConfig extends Config {
  ActiveServerGroupConfig[] getActiveServerGroupArray();

  ActiveServerGroupConfig getActiveServerGroupForL2(String l2Name);

  int getActiveServerGroupCount();
}
