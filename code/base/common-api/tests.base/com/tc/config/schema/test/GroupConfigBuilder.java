/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.schema.test;

import java.util.HashMap;
import java.util.Map;

public class GroupConfigBuilder extends BaseConfigBuilder {

  private final String         groupName;
  private MembersConfigBuilder members;
  private HaConfigBuilder      ha;

  public GroupConfigBuilder(String groupName) {
    super(5, new String[0]);
    this.groupName = groupName;
  }

  public void setMembers(MembersConfigBuilder members) {
    this.members = members;
  }

  public void setHa(HaConfigBuilder ha) {
    this.ha = ha;
  }

  @Override
  public String toString() {
    String out = "";

    Map attr = new HashMap();
    if (groupName != null) attr.put("group-name", groupName);

    out += openElement("mirror-group", attr);

    out += this.members.toString();

    if (ha != null) {
      out += this.ha.toString();
    }

    out += closeElement("mirror-group");

    return out;
  }
}
