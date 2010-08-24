/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.bundles;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import com.terracottatech.config.Module;

public class ToolkitVersion {

  private final int major;
  private final int minor;

  public ToolkitVersion(int major, int minor) {
    this.major = major;
    this.minor = minor;
  }

  public ToolkitVersion(String major, String minor) {
    this(parse(major), parse(minor));
  }

  private static int parse(String num) {
    num = num.trim();
    return Integer.parseInt(num);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof ToolkitVersion) {
      ToolkitVersion other = (ToolkitVersion) obj;
      return new EqualsBuilder().append(minor, other.minor).append(major, this.major).isEquals();
    }

    return false;
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder().append(major).append(minor).toHashCode();
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "(" + major + "." + minor + ")";
  }

  public int getMajor() {
    return major;
  }

  public int getMinor() {
    return minor;
  }

  public Module asModule() {
    Module module = Module.Factory.newInstance();
    module.setGroupId(ToolkitConstants.GROUP_ID);
    module.setName(ToolkitConstants.ARTIFACT_ID_PREFIX + major + "." + minor);
    module.setVersion(null);
    return module;
  }

  public ToolkitVersion nextMinorVersion() {
    return new ToolkitVersion(major, minor + 1);
  }

}
