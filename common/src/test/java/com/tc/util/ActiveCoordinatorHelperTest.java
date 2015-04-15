/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.util;

import com.tc.object.config.schema.L2DSOConfigObject;
import com.tc.test.TCTestCase;
import com.terracottatech.config.MirrorGroup;
import com.terracottatech.config.Server;

import java.util.Arrays;

public class ActiveCoordinatorHelperTest extends TCTestCase {
  public void testBasic() {
    int groupCount = 3;
    String[] serverNames = { "server1", "server2", "server3", "server4", "server5", "server6" };

    MirrorGroup[] mirrorGroups = new MirrorGroup[3];
    for (int i = 0; i < groupCount; i++) {
      mirrorGroups[i] = MirrorGroup.Factory.newInstance();

      for (int j = 0; j < 2; j++) {
        Server server = mirrorGroups[i].addNewServer();
        server.setName(serverNames[i * 2 + j]);
      }
    }

    MirrorGroup[] mirrorGroupsReturned = ActiveCoordinatorHelper.generateGroupNames(mirrorGroups);

    Assert.assertEquals(groupCount, mirrorGroupsReturned.length);

    for (int i = 0; i < groupCount; i++) {
      Assert.assertEquals(ActiveCoordinatorHelper.GROUP_NAME_PREFIX + i, mirrorGroupsReturned[i].getGroupName());
      String[] members = { serverNames[2 * i], serverNames[2 * i + 1] };
      Assert.assertTrue(Arrays.equals(members, L2DSOConfigObject.getServerNames(mirrorGroupsReturned[i])));
    }
  }

  public void testWithGroupNamesSet() {
    int groupCount = 3;
    String[] groupNames = { "group2", "group1", "group0" };
    String[] serverNames = { "server1", "server2", "server3", "server4", "server5", "server6" };

    MirrorGroup[] mirrorGroups = new MirrorGroup[3];
    for (int i = 0; i < groupCount; i++) {
      mirrorGroups[i] = MirrorGroup.Factory.newInstance();

      for (int j = 0; j < 2; j++) {
        Server server = mirrorGroups[i].addNewServer();
        server.setName(serverNames[i * 2 + j]);
      }

      mirrorGroups[i].setGroupName(groupNames[i]);
    }

    MirrorGroup[] mirrorGroupsReturned = ActiveCoordinatorHelper.generateGroupNames(mirrorGroups);

    Assert.assertEquals(groupCount, mirrorGroupsReturned.length);

    for (int i = groupCount - 1; i <= 0; i--) {
      Assert.assertEquals(groupNames[i], mirrorGroupsReturned[i].getGroupName());
      String[] members = { serverNames[2 * i], serverNames[2 * i + 1] };
      Assert.assertTrue(Arrays.equals(members, L2DSOConfigObject.getServerNames(mirrorGroupsReturned[i])));
    }
  }

  public void testWithSomeGroupNamesSet() {
    int groupCount = 3;
    String[] groupNames = { "group0", "wont-use", "group2" };
    String[] serverNames = { "server1", "server2", "server3", "server4", "server5", "server6" };

    MirrorGroup[] mirrorGroups = new MirrorGroup[3];
    for (int i = 0; i < groupCount; i++) {
      mirrorGroups[i] = MirrorGroup.Factory.newInstance();

      for (int j = 0; j < 2; j++) {
        Server server = mirrorGroups[i].addNewServer();
        server.setName(serverNames[i * 2 + j]);
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

    Assert.assertTrue(Arrays.equals(members, L2DSOConfigObject.getServerNames(mirrorGroupsReturned[2])));
  }
}
