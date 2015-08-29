/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema;

import org.terracotta.config.Servers;

import java.util.List;

public interface ActiveServerGroupsConfig extends Config<Servers> {
  List<ActiveServerGroupConfig> getActiveServerGroups();

  ActiveServerGroupConfig getActiveServerGroupForL2(String l2Name);

  int getActiveServerGroupCount();
}
