/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.activepassive;

import com.tc.test.MultipleServersTestSetupManager;

public class ActivePassiveTestSetupManager extends MultipleServersTestSetupManager {

  public void setServerCrashMode(String mode) {
    this.crashMode = new ActivePassiveCrashMode(mode);
  }

  public String getGroupServerShareDataMode(int groupIndex) {
    if (groupIndex != 0) throw new AssertionError("Only one group can be present in case of Active Passive Tests");
    return getServerSharedDataMode();
  }

  public int getActiveServerGroupCount() {
    return 1;
  }

  public int getGroupElectionTime(int groupIndex) {
    if (groupIndex != 0) throw new AssertionError("Only one group can be present in case of Active Passive Tests");
    return getElectionTime();
  }

  public int getGroupMemberCount(int groupIndex) {
    if (groupIndex != 0) throw new AssertionError("Only one group can be present in case of Active Passive Tests");
    return getServerCount();
  }

  public String getGroupName(int groupIndex) {
    if (groupIndex != 0) throw new AssertionError("Only one group can be present in case of Active Passive Tests");
    return getGroupName(groupIndex);
  }
}
