/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
