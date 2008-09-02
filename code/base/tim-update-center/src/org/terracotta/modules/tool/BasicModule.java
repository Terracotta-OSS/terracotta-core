/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.tool;

import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.net.URL;
import java.util.Map;

public class BasicModule extends AbstractModule implements BasicAttributes {

  private final Module              owner;
  private final Map<String, Object> attributes;
  private final AttributesHelper    attributesHelper;

  public BasicModule(Module owner, Map<String, Object> attributes) {
    this.owner = owner;
    this.attributes = attributes;
    this.attributesHelper = new AttributesHelper(this.attributes);

    groupId = attributesHelper.getAttrValueAsString("groupId", StringUtils.EMPTY);
    artifactId = attributesHelper.getAttrValueAsString("artifactId", StringUtils.EMPTY);
    version = attributesHelper.getAttrValueAsString("version", StringUtils.EMPTY);
  }

  public Module owner() {
    return owner;
  }

  public String filename() {
    return attributesHelper.filename();
  }

  public File installPath() {
    return attributesHelper.installPath();
  }

  public URL repoUrl() {
    return attributesHelper.repoUrl();
  }

  public boolean isInstalled(File repository) {
    return attributesHelper.isInstalled(repository);
  }

}
