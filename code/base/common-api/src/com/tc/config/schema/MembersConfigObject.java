/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.schema;

import com.tc.config.schema.context.ConfigContext;
import com.terracottatech.config.Members;

public class MembersConfigObject extends BaseConfigObject implements MembersConfig {
  private final String[] memberArray;

  public MembersConfigObject(ConfigContext context) {
    super(context);
    context.ensureRepositoryProvides(Members.class);
    Members members = (Members) context.bean();
    memberArray = members.getMemberArray();
  }

  public String[] getMemberArray() {
    return this.memberArray;
  }
}
