/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.tool;

import org.apache.commons.lang.StringUtils;
import org.jdom.Element;

/**
 * Composite unique identifier for TIMs.
 * 
 * @author Jason Voegele (jvoegele@terracotta.org)
 */
public class ModuleId implements Comparable {

  public static final String DEFAULT_GROUPID = "org.terracotta.modules";

  private final String       groupId;
  private final String       artifactId;
  private final String       version;

  ModuleId(String groupId, String artifactId, String version) {
    assert groupId != null : "groupId is null";
    assert artifactId != null : "artifactId is null";
    assert version != null : "version is null";
    this.groupId = groupId.trim();
    this.artifactId = artifactId.trim();
    this.version = version.trim();
  }

  public boolean isUsingDefaultGroupId() {
    return DEFAULT_GROUPID.equals(groupId);
  }

  public String getGroupId() {
    return groupId;
  }

  public String getArtifactId() {
    return artifactId;
  }

  public String getVersion() {
    return version;
  }

  public String getSymbolicName() {
    return ModuleId.computeSymbolicName(groupId, artifactId);
  }

  /**
   * Sibling Modules are Modules with matching groupId and artifactId.
   */
  public boolean isSibling(ModuleId id) {
    return this.groupId.equals(id.getGroupId()) && this.artifactId.equals(id.getArtifactId());
  }

  String sortableVersion() {
    String v = version.replaceAll("-.+$", "");
    String q = version.replaceFirst(v, "").replaceFirst("-", "");
    String[] cv = v.split("\\.");
    for (int i = 0; i < cv.length; i++) {
      cv[i] = StringUtils.leftPad(cv[i], 3, '0');
    }
    return StringUtils.join(cv, '.') + "-" + q;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((artifactId == null) ? 0 : artifactId.hashCode());
    result = prime * result + ((groupId == null) ? 0 : groupId.hashCode());
    result = prime * result + ((version == null) ? 0 : version.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    final ModuleId other = (ModuleId) obj;
    if (artifactId == null) {
      if (other.artifactId != null) return false;
    } else if (!artifactId.equals(other.artifactId)) return false;
    if (groupId == null) {
      if (other.groupId != null) return false;
    } else if (!groupId.equals(other.groupId)) return false;
    if (version == null) {
      if (other.version != null) return false;
    } else if (!version.equals(other.version)) return false;
    return true;
  }

  public static ModuleId create(String groupId, String artifactId, String version) {
    return new ModuleId(groupId, artifactId, version);
  }

  public static ModuleId create(Element element) {
    String groupId = element.getAttributeValue("groupId");
    String artifactId = element.getAttributeValue("artifactId");
    String version = element.getAttributeValue("version");
    return new ModuleId(groupId, artifactId, version);
  }

  public static String computeSymbolicName(String groupId, String artifactId) {
    StringBuffer buffer = new StringBuffer();
    if (groupId.length() > 0) buffer.append(groupId).append(".");
    buffer.append(artifactId);
    return buffer.toString();
  }

  public int compareTo(Object obj) {
    ModuleId other = (ModuleId) obj;
    String v1, v2;
    // comparing siblings:
    // if same version numbers, but one has a qualifier
    // make the one with the qualifier older
    if (this.getSymbolicName().equals(other.getSymbolicName())) {
      v1 = this.sortableVersion();
      v2 = other.sortableVersion();
      int value = v1.compareTo(v2);
      if (v1.startsWith(v2) || v2.startsWith(v1)) value = -value;
      return value;
    }
    // comparing non-siblings:
    v1 = this.toSortableString();
    v2 = other.toSortableString();
    return v1.compareTo(v2);
  }

  private String toSortableString() {
    return ModuleId.computeSymbolicName(groupId, artifactId) + "-" + sortableVersion();
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + ": " + getSymbolicName() + " [" + version + "]";
  }

  public String toDigestString() {
    String digest = artifactId.concat(" (").concat(version).concat(")");
    if (!isUsingDefaultGroupId()) digest = digest.concat(" [" + groupId + "]");
    return digest;
  }
}
