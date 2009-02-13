/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.schema.test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MembersConfigBuilder extends BaseConfigBuilder {
  private static final String[] ALL_PROPERTIES = new String[0];
  private final List            members;

  public MembersConfigBuilder() {
    super(7, ALL_PROPERTIES);
    this.members = new ArrayList();
  }

  public void addMember(String member) {
    this.members.add(member);
  }

  public void addMembers(String[] membersList) {
    for (int i = 0; i < membersList.length; i++) {
      this.members.add(membersList[i]);
    }
  }

  public String toString() {
    String out = "";

    out += openElement("members");

    for (Iterator iter = this.members.iterator(); iter.hasNext();) {
      out += indent() + "<member>";
      out += iter.next();
      out += "</member>";
    }

    out += closeElement("members");

    return out;
  }
}
