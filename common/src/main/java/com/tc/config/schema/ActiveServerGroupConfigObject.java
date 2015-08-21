/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema;

import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.L2ConfigurationSetupManagerImpl;
import com.tc.net.GroupID;
import org.terracotta.config.Server;
import org.terracotta.config.Servers;

import java.util.HashSet;
import java.util.Set;

public class ActiveServerGroupConfigObject implements ActiveServerGroupConfig {

  private final Servers s;

  private GroupID             groupId;
  private String              grpName;
  private final Set<String>   members;

  public ActiveServerGroupConfigObject(Servers s, L2ConfigurationSetupManagerImpl setupManager)
      throws ConfigurationSetupException {
    this.s = s;
    members = new HashSet<>();
    for(Server server : s.getServer()) {
      members.add(server.getName());
    }
  }

  public void setGroupId(GroupID groupId) {
    this.groupId = groupId;
  }

  @Override
  public int getElectionTimeInSecs() {
    //TODO fix the election time
    return 5;
  }

  public void setGroupName(String groupName) {
    this.grpName = groupName;
  }

  @Override
  public String getGroupName() {
    return grpName;
  }

  @Override
  public String[] getMembers() {
    return this.members.toArray(new String[members.size()]);
  }

  @Override
  public GroupID getGroupId() {
    return this.groupId;
  }

  @Override
  public boolean isMember(String l2Name) {
    return members.contains(l2Name);
  }

  public static void createDefaultMirrorGroup(Servers servers) {
    //TODO fix this,
    //DO nothing
  }

  @Override
  public Servers getBean() {
    return s;
  }
}
