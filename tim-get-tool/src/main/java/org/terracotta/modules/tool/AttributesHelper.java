/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.tool;

import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;

class AttributesHelper implements Installable {

  private final Map<String, Object> attributes;
  private final URI                 relativeUrlBase;

  public AttributesHelper(Map<String, Object> attributes) {
    this(attributes, URI.create("/"));
  }

  public AttributesHelper(Map<String, Object> attributes, URI relativeUrlBase) {
    this.attributes = attributes;
    this.relativeUrlBase = relativeUrlBase;
  }

  public AttributesHelper(Map<String, Object> attributes, String relativeUrlBase) throws URISyntaxException {
    this(attributes, new URI(relativeUrlBase));
  }

  static String getAttrValueAsString(Map<String, Object> attributes, String key, String altvalue) {
    String value = (String) attributes.get(key);
    return StringUtils.isEmpty(value) && (altvalue != null) ? altvalue : value.trim();
  }

  static File getAttrValueAsFile(Map<String, Object> attributes, String key, File altvalue) {
    String value = (String) attributes.get(key);
    return StringUtils.isEmpty(value) && (altvalue != null) ? altvalue : new File(value);
  }

  String getAttrValueAsString(String key, String altvalue) {
    return getAttrValueAsString(attributes, key, altvalue);
  }

  URL getAttrValueAsUrl(String key, URL altvalue) {
    try {
      String value = (String) attributes.get(key);
      if (value != null) {
        value = value.trim();
      }
      if (StringUtils.isEmpty(value)) {
        if (altvalue != null) {
          return altvalue;
        } else {
          return new URL("");
        }
      }

      URI uri = new URI(value);
      if (uri.isAbsolute()) {
        return uri.toURL();
      } else {
        return new URI(this.relativeUrlBase + "/" + uri).normalize().toURL();
      }
    } catch (MalformedURLException e) {
      throw new IllegalStateException(e);
    } catch (URISyntaxException e) {
      throw new IllegalStateException(e);
    }
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
    return getAttrValueAsUrl("repoURL", null);
  }

  public File installLocationInRepository(File repositoryRoot) {
    File directory = new File(repositoryRoot, installPath().toString());
    return new File(directory, filename());
  }

  public boolean isInstalled(File repository) {
    File p0 = new File(repository, installPath().toString());
    p0 = new File(p0, filename());
    File p1 = new File(repository, filename());
    return p0.exists() || p1.exists();
  }

}
