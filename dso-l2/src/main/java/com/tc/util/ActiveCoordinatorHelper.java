/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.util;


import com.tc.config.schema.ActiveServerGroupConfigObject;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.net.GroupID;

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


  private static TreeMap<String, ActiveServerGroupConfigObject> generateCandidateGroupNames(ActiveServerGroupConfigObject[] asgcos) {
    TreeMap<String, ActiveServerGroupConfigObject> groupNamesToGroup = new TreeMap<>();

    for (ActiveServerGroupConfigObject asgco : asgcos) {
      String groupName = null;
      if (groupNameNotSet(asgco)) {
        groupName = getGroupNameFrom(asgco.getMembers());
      } else {
        groupName = asgco.getGroupName();
      }

      groupNamesToGroup.put(groupName, asgco);
    }
    return groupNamesToGroup;
  }


  private static boolean groupNameNotSet(ActiveServerGroupConfigObject asgco) {
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
