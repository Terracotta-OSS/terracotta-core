/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util;

import com.tc.config.schema.ActiveServerGroupConfigObject;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.net.GroupID;
import com.terracottatech.config.MirrorGroup;

import java.util.Arrays;
import java.util.Map.Entry;
import java.util.TreeMap;

public class ActiveCoordinatorHelper {
  public static final String GROUP_NAME_PREFIX = "Tc-Group-";

  public static ActiveServerGroupConfigObject[] generateGroupInfo(ActiveServerGroupConfigObject[] originalGroupInfos)
      throws ConfigurationSetupException {
    TreeMap<String, ActiveServerGroupConfigObject> candidateGroupNames = generateCandidateGroupNames(originalGroupInfos);

    if (originalGroupInfos.length != candidateGroupNames.size()) { throw new ConfigurationSetupException(
                                                                                                         "The group names specified are same "
                                                                                                             + candidateGroupNames
                                                                                                                 .keySet()); }
    // Generate Group Info
    ActiveServerGroupConfigObject[] groupInfos = new ActiveServerGroupConfigObject[originalGroupInfos.length];
    int groupID = 0;
    for (Entry<String, ActiveServerGroupConfigObject> entry : candidateGroupNames.entrySet()) {
      ActiveServerGroupConfigObject groupInfo = entry.getValue();
      if (groupNameNotSet(groupInfo)) {
        groupInfo.setGroupName(GROUP_NAME_PREFIX + groupID);
      }
      groupInfo.setGroupId(new GroupID(groupID));
      groupInfos[groupID] = groupInfo;
      groupID++;
    }

    return groupInfos;
  }

  public static MirrorGroup[] generateGroupNames(MirrorGroup[] originalGroupInfos) {
    TreeMap<String, MirrorGroup> candidateGroupNames = generateCandidateGroupNames(originalGroupInfos);

    // Setting actual group info
    MirrorGroup[] groupInfos = new MirrorGroup[originalGroupInfos.length];
    int counter = 0;
    for (Entry<String, MirrorGroup> entry : candidateGroupNames.entrySet()) {
      MirrorGroup groupInfo = entry.getValue();
      if (groupNameNotSet(groupInfo)) {
        groupInfo.setGroupName(GROUP_NAME_PREFIX + counter);
      }
      groupInfos[counter] = groupInfo;
      counter++;
    }

    return groupInfos;
  }

  private static TreeMap<String, ActiveServerGroupConfigObject> generateCandidateGroupNames(ActiveServerGroupConfigObject[] asgcos) {
    TreeMap<String, ActiveServerGroupConfigObject> groupNamesToGroup = new TreeMap<String, ActiveServerGroupConfigObject>();

    for (ActiveServerGroupConfigObject asgco : asgcos) {
      String groupName = null;
      if (groupNameNotSet(asgco)) {
        groupName = getGroupNameFrom(asgco.getMembers().getMemberArray());
      } else {
        groupName = asgco.getGroupName();
      }

      groupNamesToGroup.put(groupName, asgco);
    }
    return groupNamesToGroup;
  }

  private static TreeMap<String, MirrorGroup> generateCandidateGroupNames(MirrorGroup[] mirrorGroups) {
    TreeMap<String, MirrorGroup> groupNamesToGroup = new TreeMap<String, MirrorGroup>();

    for (MirrorGroup mirrorGroup : mirrorGroups) {
      String groupName = null;
      if (groupNameNotSet(mirrorGroup)) {
        groupName = getGroupNameFrom(mirrorGroup.getMembers().getMemberArray());
      } else {
        groupName = mirrorGroup.getGroupName();
      }

      groupNamesToGroup.put(groupName, mirrorGroup);
    }
    return groupNamesToGroup;
  }

  private static boolean groupNameNotSet(ActiveServerGroupConfigObject asgco) {
    return asgco.getGroupName() == null || asgco.getGroupName() == "";
  }

  private static boolean groupNameNotSet(MirrorGroup asgco) {
    return asgco.getGroupName() == null || asgco.getGroupName() == "";
  }

  private static String getGroupNameFrom(String[] members) {
    String[] temp = new String[members.length];
    for (int i = 0; i < temp.length; i++) {
      temp[i] = members[i];
    }
    Arrays.sort(temp);

    StringBuffer groupName = new StringBuffer();
    for (String element : temp) {
      groupName.append(element);
    }
    return groupName.toString();
  }

}
