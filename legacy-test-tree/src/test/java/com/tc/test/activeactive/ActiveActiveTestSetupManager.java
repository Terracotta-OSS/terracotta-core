/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.activeactive;

import com.tc.test.MultipleServersTestSetupManager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ActiveActiveTestSetupManager extends MultipleServersTestSetupManager {

  private final List          activeServerGroups = new ArrayList();
  private static final String GROUP_NAME_PREFIX  = "mirror-group-";
  private static int          groupCount;

  public void addActiveServerGroup(int membersCount, String local_activePassiveMode, int local_electionTime) {
    addActiveServerGroup(GROUP_NAME_PREFIX + groupCount++, membersCount, local_activePassiveMode, local_electionTime);
  }

  public void addActiveServerGroup(String groupName, int membersCount, String local_activePassiveMode,
                                   int local_electionTime) {
    this.activeServerGroups.add(new Group(groupName, membersCount, local_activePassiveMode, local_electionTime));
  }

  public int getActiveServerGroupCount() {
    checkServerCount();
    return this.activeServerGroups.size();
  }

  public int getGroupMemberCount(int groupIndex) {
    checkServerCount();
    return ((Group) this.activeServerGroups.get(groupIndex)).getMemberCount();
  }

  public int getGroupElectionTime(int groupIndex) {
    checkServerCount();
    return ((Group) this.activeServerGroups.get(groupIndex)).getElectionTime();
  }

  public String getGroupName(int groupIndex) {
    checkServerCount();
    return ((Group) this.activeServerGroups.get(groupIndex)).getGroupName();
  }

  public String getGroupServerShareDataMode(int groupIndex) {
    checkServerCount();
    return ((Group) this.activeServerGroups.get(groupIndex)).getMode();
  }

  private void checkServerCount() {
    int serverCount = this.getServerCount();
    if (this.activeServerGroups.size() == 0) {
      addActiveServerGroup(serverCount, this.getServerSharedDataMode(), this.electionTime);
    }

    int totalMemberCount = 0;
    for (Iterator iter = this.activeServerGroups.iterator(); iter.hasNext();) {
      Group grp = (Group) iter.next();
      totalMemberCount += grp.getMemberCount();
    }
    if (totalMemberCount != serverCount) { throw new AssertionError(
                                                                    "Number of servers indicated does not match the number of active-server-group members:  totalMemberCount=["
                                                                        + totalMemberCount + "] serverCount=["
                                                                        + serverCount + "]."); }
  }

  public void setServerCrashMode(String mode) {
    this.crashMode = new ActiveActiveCrashMode(mode);
  }

  private static class Group {
    private final String groupName;
    private final int    memberCount;
    private final String groupPersistenceMode;
    private final int    groupElectionTime;

    public Group(String groupName, int memberCount, String persistenceMode, int electionTime) {
      this.groupName = groupName;
      this.memberCount = memberCount;
      groupPersistenceMode = persistenceMode;
      groupElectionTime = electionTime;
    }

    public String getGroupName() {
      return this.groupName;
    }

    public int getMemberCount() {
      return this.memberCount;
    }

    public String getMode() {
      return this.groupPersistenceMode;
    }

    public int getElectionTime() {
      return this.groupElectionTime;
    }
  }
}
