/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

import com.tc.test.TCTestCase;
import com.terracottatech.config.Members;
import com.terracottatech.config.MirrorGroup;

import java.util.Arrays;

public class ActiveCoordinatorHelperTest extends TCTestCase {
  public void testBasic() {
    int groupCount = 3;
    String[] serverNames = { "server1", "server2", "server3", "server4", "server5", "server6" };

    MirrorGroup[] mirrorGroups = new MirrorGroup[3];
    for (int i = 0; i < groupCount; i++) {
      mirrorGroups[i] = MirrorGroup.Factory.newInstance();
      Members members = mirrorGroups[i].addNewMembers();

      for (int j = 0; j < 2; j++) {
        members.addMember(serverNames[i * 2 + j]);
      }
    }

    MirrorGroup[] mirrorGroupsReturned = ActiveCoordinatorHelper.generateGroupNames(mirrorGroups);

    Assert.assertEquals(groupCount, mirrorGroupsReturned.length);

    for (int i = 0; i < groupCount; i++) {
      Assert.assertEquals(ActiveCoordinatorHelper.GROUP_NAME_PREFIX + i, mirrorGroupsReturned[i].getGroupName());
      String[] members = { serverNames[2 * i], serverNames[2 * i + 1] };
      Assert.assertTrue(Arrays.equals(members, mirrorGroupsReturned[i].getMembers().getMemberArray()));
    }
  }

  public void testWithGroupNamesSet() {
    int groupCount = 3;
    String[] groupNames = { "group2", "group1", "group0" };
    String[] serverNames = { "server1", "server2", "server3", "server4", "server5", "server6" };

    MirrorGroup[] mirrorGroups = new MirrorGroup[3];
    for (int i = 0; i < groupCount; i++) {
      mirrorGroups[i] = MirrorGroup.Factory.newInstance();
      Members members = mirrorGroups[i].addNewMembers();

      for (int j = 0; j < 2; j++) {
        members.addMember(serverNames[i * 2 + j]);
      }

      mirrorGroups[i].setGroupName(groupNames[i]);
    }

    MirrorGroup[] mirrorGroupsReturned = ActiveCoordinatorHelper.generateGroupNames(mirrorGroups);

    Assert.assertEquals(groupCount, mirrorGroupsReturned.length);

    for (int i = groupCount - 1; i <= 0; i--) {
      Assert.assertEquals(groupNames[i], mirrorGroupsReturned[i].getGroupName());
      String[] members = { serverNames[2 * i], serverNames[2 * i + 1] };
      Assert.assertTrue(Arrays.equals(members, mirrorGroupsReturned[i].getMembers().getMemberArray()));
    }
  }

  public void testWithSomeGroupNamesSet() {
    int groupCount = 3;
    String[] groupNames = { "group0", "wont-use", "group2" };
    String[] serverNames = { "server1", "server2", "server3", "server4", "server5", "server6" };

    MirrorGroup[] mirrorGroups = new MirrorGroup[3];
    for (int i = 0; i < groupCount; i++) {
      mirrorGroups[i] = MirrorGroup.Factory.newInstance();
      Members members = mirrorGroups[i].addNewMembers();

      for (int j = 0; j < 2; j++) {
        members.addMember(serverNames[i * 2 + j]);
      }

      if (i != 1) {
        mirrorGroups[i].setGroupName(groupNames[i]);
      }
    }

    MirrorGroup[] mirrorGroupsReturned = ActiveCoordinatorHelper.generateGroupNames(mirrorGroups);

    Assert.assertEquals(groupCount, mirrorGroupsReturned.length);
    Assert.assertEquals(groupNames[0], mirrorGroupsReturned[0].getGroupName());
    Assert.assertEquals(groupNames[2], mirrorGroupsReturned[1].getGroupName());

    Assert.assertEquals(ActiveCoordinatorHelper.GROUP_NAME_PREFIX + "2", mirrorGroupsReturned[2].getGroupName());
    String[] members = { serverNames[2], serverNames[3] };

    Assert.assertTrue(Arrays.equals(members, mirrorGroupsReturned[2].getMembers().getMemberArray()));
  }
}
