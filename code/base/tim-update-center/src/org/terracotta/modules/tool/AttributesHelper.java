/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.tool;

import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

class AttributesHelper implements BasicAttributes {

  private final Map<String, Object> attributes;

  public AttributesHelper(Map<String, Object> attributes) {
    this.attributes = attributes;
  }

  static String getAttrValueAsString(Map<String, Object> attributes, String key, String altvalue) {
    String value = (String) attributes.get(key);
    return StringUtils.isEmpty(value) && (altvalue != null) ? altvalue : value.trim();
  }

  static URL getAttrValueAsUrl(Map<String, Object> attributes, String key, URL altvalue) {
    try {
      String value = (String) attributes.get(key);
      return StringUtils.isEmpty(value) && (altvalue != null) ? altvalue : new URL(value);
    } catch (MalformedURLException e) {
      throw new IllegalStateException(e);
    }
  }

  static File getAttrValueAsFile(Map<String, Object> attributes, String key, File altvalue) {
    String value = (String) attributes.get(key);
    return StringUtils.isEmpty(value) && (altvalue != null) ? altvalue : new File(value);
  }

  String getAttrValueAsString(String key, String altvalue) {
    return getAttrValueAsString(attributes, key, altvalue);
  }

  URL getAttrValueAsUrl(String key, URL altvalue) {
    return getAttrValueAsUrl(attributes, key, altvalue);
  }

  File getAttrValueAsFile(String key, File altvalue) {
    String value = (String) attributes.get(key);
    return StringUtils.isEmpty(value) ? new File(altvalue.toString()) : new File(value);
  }

  public String filename() {
    return getAttrValueAsString(attributes, "filename", null).trim();
  }

  public File installPath() {
    return getAttrValueAsFile(attributes, "installPath", null);
  }

  public URL repoUrl() {
    return getAttrValueAsUrl(attributes, "repoURL", null);
  }

  public boolean isInstalled(File repository) {
    File p0 = new File(repository, installPath().toString());
    p0 = new File(p0, filename());
    File p1 = new File(repository, filename());
    return p0.exists() || p1.exists();
  }

}
