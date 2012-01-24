/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema;

import com.tc.net.GroupID;

public interface ActiveServerGroupConfig extends Config {
  MembersConfig getMembers();

  boolean isMember(String l2Name);

  HaConfigSchema getHaHolder();

  GroupID getGroupId();
  
  String getGroupName();
}
