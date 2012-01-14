/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.test.schema;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GroupsConfigBuilder extends BaseConfigBuilder {
  private List groups;

  public GroupsConfigBuilder() {
    super(3, new String[0]);
    this.groups = new ArrayList();
  }

  public void addGroupConfigBuilder(GroupConfigBuilder data) {
    this.groups.add(data);
  }

  public void addGroupConfigBuilder(GroupConfigBuilder[] data) {
    int numberOfGroups = data.length;
    for(int i = 0; i < numberOfGroups; i++){
      this.groups.add(data[i]);
    }
  }
  
  public void addGroupConfigBuilders(List groupConfigBuilders) {
    for (Iterator iter = groupConfigBuilders.iterator(); iter.hasNext();) {
      GroupConfigBuilder group = (GroupConfigBuilder) iter.next();
      this.groups.add(group);
    }
  }

  public String toString() {
    String out = "";

    out += openElement("mirror-groups");

    for (Iterator iter = this.groups.iterator(); iter.hasNext();) {
      GroupConfigBuilder group = (GroupConfigBuilder) iter.next();
      out += group.toString();
    }
    out += closeElement("mirror-groups");

    return out;
  }

}
