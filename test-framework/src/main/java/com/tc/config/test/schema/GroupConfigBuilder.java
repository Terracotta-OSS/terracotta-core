/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.test.schema;

import java.util.HashMap;
import java.util.Map;

public class GroupConfigBuilder extends BaseConfigBuilder {

  private final String         groupName;
  private MembersConfigBuilder members;
  private Integer              electionTime = null;

  public GroupConfigBuilder(String groupName) {
    super(5, new String[0]);
    this.groupName = groupName;
  }

  public void setMembers(MembersConfigBuilder members) {
    this.members = members;
  }

  public void setElectionTime(int value) {
    setProperty("election-time", value);
    this.electionTime = value;
  }

  @Override
  public String toString() {
    String out = "";

    Map attr = new HashMap();
    if (groupName != null) attr.put("group-name", groupName);
    if (electionTime != null) {
      attr.put("election-time", electionTime.toString());
    }

    out += openElement("mirror-group", attr);

    out += this.members.toString();

    out += closeElement("mirror-group");

    return out;
  }
}
