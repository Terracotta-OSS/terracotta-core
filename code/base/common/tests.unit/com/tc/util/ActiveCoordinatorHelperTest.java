/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

import org.apache.xmlbeans.XmlObject;

import com.tc.config.schema.ActiveServerGroupConfig;
import com.tc.config.schema.MembersConfig;
import com.tc.config.schema.NewHaConfig;
import com.tc.config.schema.dynamic.ConfigItem;
import com.tc.test.TCTestCase;
import com.terracottatech.config.ActiveServerGroup;
import com.terracottatech.config.Members;

public class ActiveCoordinatorHelperTest extends TCTestCase {

  public void testGetCoordinatorGroup1() {
    ActiveServerGroup[] groups = new ActiveServerGroup[3];

    groups[0] = ActiveServerGroup.Factory.newInstance();
    groups[0].setGroupName("15");
    Members members = groups[0].addNewMembers();
    members.addMember("server1");

    groups[1] = ActiveServerGroup.Factory.newInstance();
    groups[1].setGroupName("13");
    members = groups[1].addNewMembers();
    members.addMember("server2");

    groups[2] = ActiveServerGroup.Factory.newInstance();
    members = groups[2].addNewMembers();
    members.addMember("server3");

    Assert.assertEquals(1, ActiveCoordinatorHelper.getCoordinatorGroup(groups));
  }

  public void testGetCoordinatorGroup2() {
    ActiveServerGroupConfig[] asgcs = new ActiveServerGroupConfig[3];

    asgcs[0] = new NullActiveServerGroupConfig("2223");
    asgcs[1] = new NullActiveServerGroupConfig("timepass");
    asgcs[2] = new NullActiveServerGroupConfig("34444");

    Assert.assertEquals(0, ActiveCoordinatorHelper.getCoordinatorGroup(asgcs));
  }

  public void testGetGroupNameFrom() {
    String[] temp = new String[10];
    for (int i = 0; i < 10; i++) {
      temp[i] = "" + (9 - i);
    }

    String temp2 = ActiveCoordinatorHelper.getGroupNameFrom(temp);

    String temp3 = "";
    for (int i = 0; i < 10; i++) {
      temp3 = temp3 + i;
    }

    Assert.assertEquals(temp3, temp2);
  }

  private static class NullActiveServerGroupConfig implements ActiveServerGroupConfig {
    private final String groupName;

    public NullActiveServerGroupConfig(String groupName) {
      this.groupName = groupName;
    }

    public String getGroupName() {
      return groupName;
    }

    public NewHaConfig getHa() {
      return null;
    }

    public int getGroupId() {
      return 0;
    }

    public MembersConfig getMembers() {
      return null;
    }

    public boolean isMember(String name) {
      return false;
    }

    public void changesInItemForbidden(ConfigItem item) {
      //
    }

    public void changesInItemIgnored(ConfigItem item) {
      //
    }

    public XmlObject getBean() {
      return null;
    }
  }
}
