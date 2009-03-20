/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

import com.tc.config.schema.ActiveServerGroupConfig;
import com.terracottatech.config.MirrorGroup;

import java.util.Arrays;

public class ActiveCoordinatorHelper {

  public static int getCoordinatorGroup(MirrorGroup[] asg) {
    int grpId = -1;
    String groupName = null;

    for (int i = 0; i < asg.length; i++) {
      String grpName = asg[i].getGroupName();
      if (grpName == null) {
        grpName = getGroupNameFrom(asg[i].getMembers().getMemberArray());
      }
      Assert.assertNotNull(grpName);
      if (grpId == -1 || groupName.compareTo(grpName) > 0) {
        grpId = i;
        groupName = grpName;
      }
    }

    return grpId;
  }

  public static int getCoordinatorGroup(ActiveServerGroupConfig[] asg) {
    if (asg == null) return -1;

    int grpId = -1;
    String groupName = null;

    for (int i = 0; i < asg.length; i++) {
      String grpName = asg[i].getGroupName();
      Assert.assertNotNull(grpName);
      if (grpId == -1 || groupName.compareTo(grpName) > 0) {
        grpId = i;
        groupName = grpName;
      }
    }

    return grpId;
  }

  public static String getGroupNameFrom(String[] members) {
    String[] temp = new String[members.length];
    for (int i = 0; i < temp.length; i++) {
      temp[i] = members[i];
    }
    Arrays.sort(temp);

    StringBuffer grpName = new StringBuffer();
    for (int i = 0; i < temp.length; i++) {
      grpName.append(temp[i]);
    }
    return grpName.toString();
  }
}
