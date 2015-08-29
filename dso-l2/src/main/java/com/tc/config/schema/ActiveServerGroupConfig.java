/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema;

import com.tc.net.GroupID;
import org.terracotta.config.Servers;

public interface ActiveServerGroupConfig extends Config<Servers> {
  String[] getMembers();

  boolean isMember(String l2Name);

  GroupID getGroupId();

  String getGroupName();

  int getElectionTimeInSecs();

}
