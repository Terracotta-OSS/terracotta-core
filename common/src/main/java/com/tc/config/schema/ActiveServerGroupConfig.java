/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema;

import com.tc.net.GroupID;

public interface ActiveServerGroupConfig extends Config {
  String[] getMembers();

  boolean isMember(String l2Name);

  GroupID getGroupId();

  String getGroupName();

  int getElectionTimeInSecs();

}
