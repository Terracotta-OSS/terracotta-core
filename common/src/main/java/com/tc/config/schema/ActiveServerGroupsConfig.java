/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema;

import java.util.List;

public interface ActiveServerGroupsConfig extends Config {
  List<ActiveServerGroupConfig> getActiveServerGroups();

  ActiveServerGroupConfig getActiveServerGroupForL2(String l2Name);

  int getActiveServerGroupCount();
}
