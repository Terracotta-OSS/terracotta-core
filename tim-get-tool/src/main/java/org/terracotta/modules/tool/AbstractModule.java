/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.tool;

import org.apache.commons.lang.StringUtils;

public abstract class AbstractModule implements Comparable {

  protected String artifactId;
  protected String groupId;
  protected String version;

  public String artifactId() {
    if (StringUtils.isEmpty(artifactId)) throw new NullPointerException("artifactId can't be empty or null");
    return artifactId;
  }

  public String groupId() {
    return (StringUtils.isEmpty(groupId)) ? "" : groupId;
  }

  public String version() {
    if (StringUtils.isEmpty(version)) throw new NullPointerException("version can't be empty or null");
    return version;
  }

  public String symbolicName() {
    StringBuffer name = new StringBuffer();
    if (!StringUtils.isEmpty(groupId())) name.append(groupId()).append('.');
    name.append(artifactId());
    return name.toString();
  }

  protected boolean isSibling(AbstractModule other) {
    return symbolicName().equals(other.symbolicName()) && !version().equals(other.version());
  }

  private String sortableVersion() {
    String v = version().replaceAll("-.+$", "");
    String q = version().replaceFirst(v, "").replaceFirst("-", "");
    String[] cv = v.split("\\.");
    int MAXPAD = 10;
    for (int i = 0; i < cv.length; i++) {
      cv[i] = StringUtils.leftPad(cv[i], MAXPAD, '0');
    }
    return StringUtils.join(cv, '.') + q;
  }

  private String toSortableString() {
    return symbolicName() + "-" + sortableVersion();
  }

  public boolean isOlder(AbstractModule other) {
    if (!symbolicName().equals(other.symbolicName())) throw new UnsupportedOperationException();
    return compareTo(other) < 0;
  }

  public int compareTo(Object o) {
    AbstractModule other = (AbstractModule) o;
    String v1, v2;
    // comparing siblings:
    // if same version numbers, but one has a qualifier make the one with the qualifier older
    if (isSibling(other)) {
      v1 = sortableVersion();
      v2 = other.sortableVersion();
      int value = v1.compareTo(v2);
      if (v1.startsWith(v2) || v2.startsWith(v1)) value = -value;
      return value;
    }

    // comparing non-siblings:
    v1 = toSortableString();
    v2 = other.toSortableString();
    return v1.compareTo(v2);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + artifactId().hashCode();
    result = prime * result + groupId().hashCode();
    result = prime * result + version().hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    final AbstractModule other = (AbstractModule) obj;
    return symbolicName().equals(other.symbolicName()) && version().equals(other.version());
  }

  @Override
  public String toString() {
    return symbolicName() + " (" + version() + ")";
  }

}
