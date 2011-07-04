/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.tool;

import org.apache.commons.lang.StringUtils;

import java.util.Map;

public class Reference extends AbstractModule {

  private final Module              owner;
  private final Map<String, Object> attributes;

  public Reference(Module owner, Map<String, Object> attributes) {
    this.owner = owner;
    this.attributes = attributes;
    AttributesHelper attributesHelper = new AttributesHelper(this.attributes);

    groupId = attributesHelper.getAttrValueAsString("groupId", StringUtils.EMPTY);
    artifactId = attributesHelper.getAttrValueAsString("artifactId", StringUtils.EMPTY);
    version = attributesHelper.getAttrValueAsString("version", StringUtils.EMPTY);
  }

  public Module owner() {
    return owner;
  }

}
