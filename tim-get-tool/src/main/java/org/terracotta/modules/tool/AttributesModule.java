/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.tool;

import org.apache.commons.lang.StringUtils;

import com.google.inject.Inject;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.Map;

public abstract class AttributesModule extends AbstractModule implements Installable {

  private final Map<String, Object> attributes;
  private final AttributesHelper    attributesHelper;
  private final URI                 relativeUrlBase;

  @Inject
  public AttributesModule(Map<String, Object> attributes, URI relativeUrlBase) {
    this.attributes = attributes;
    this.relativeUrlBase = relativeUrlBase;
    this.attributesHelper = new AttributesHelper(attributes, relativeUrlBase);

    groupId = attributesHelper.getAttrValueAsString("groupId", StringUtils.EMPTY);
    artifactId = attributesHelper.getAttrValueAsString("artifactId", StringUtils.EMPTY);
    version = attributesHelper.getAttrValueAsString("version", StringUtils.EMPTY);
  }

  protected Map<String, Object> getAttributes() {
    return this.attributes;
  }

  protected AttributesHelper getAttributesHelper() {
    return this.attributesHelper;
  }

  protected URI getRelativeURlBase() {
    return this.relativeUrlBase;
  }

  public String filename() {
    return attributesHelper.filename();
  }

  public File installPath() {
    return attributesHelper.installPath();
  }

  public File installLocationInRepository(File repositoryRoot) {
    return attributesHelper.installLocationInRepository(repositoryRoot);
  }

  public URL repoUrl() {
    return attributesHelper.repoUrl();
  }

  public boolean isInstalled(File repository) {
    return attributesHelper.isInstalled(repository);
  }

}
