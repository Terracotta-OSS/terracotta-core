/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.bundles;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

public class ToolkitVersion {

  private final int     major;
  private final int     minor;
  private final boolean isEE;

  public ToolkitVersion(int major, int minor, boolean ee) {
    this.major = major;
    this.minor = minor;
    this.isEE = ee;
  }

  public ToolkitVersion(String major, String minor, String ee) {
    this(parse(major), parse(minor), ee != null && ee.equals("-ee"));
  }

  private static int parse(String num) {
    num = num.trim();
    return Integer.parseInt(num);
  }

  public boolean isEE() {
    return isEE;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof ToolkitVersion) {
      ToolkitVersion other = (ToolkitVersion) obj;
      return new EqualsBuilder().append(minor, other.minor).append(major, this.major).append(isEE, other.isEE)
          .isEquals();
    }

    return false;
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder().append(major).append(minor).append(isEE).toHashCode();
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "(" + major + "." + minor + (isEE() ? "-ee)" : ")");
  }

  public int getMajor() {
    return major;
  }

  public int getMinor() {
    return minor;
  }

  public Module asModule() {
    Module module = new Module();
    module.setGroupId(ToolkitConstants.GROUP_ID);
    module.setArtifactId(ToolkitConstants.ARTIFACT_ID_PREFIX + major + "." + minor + (isEE() ? "-ee" : ""));
    module.setVersion(null);
    return module;
  }

  public ToolkitVersion nextMinorVersion() {
    return new ToolkitVersion(major, minor + 1, isEE);
  }

}
