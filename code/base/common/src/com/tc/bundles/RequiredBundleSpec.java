/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.bundles;

import org.osgi.framework.BundleException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class RequiredBundleSpec {
  private static final String PROP_KEY_RESOLUTION         = "resolution";
  private static final String PROP_KEY_BUNDLE_VERSION     = "bundle-version";
  private static final String REQUIRE_BUNDLE_EXPR_MATCHER = "([A-Za-z0-9._\\-]+(;resolution:=\"optional\")?(;bundle-version:=(\"[A-Za-z0-9.]+\"|\"\\[[A-Za-z0-9.]+,[A-Za-z0-9.]+\\]\"))?)";

  private final String        symbolicName;
  private Map                 attributes;

  public static final String[] parseList(final String source) throws BundleException {
    ArrayList list = new ArrayList();
    if (source != null) {
      final String spec     = source.replaceAll(" ", "");
      final String regex    = REQUIRE_BUNDLE_EXPR_MATCHER;
      final Pattern pattern = Pattern.compile(regex);
      final Matcher matcher = pattern.matcher(spec);
      StringBuffer check    = new StringBuffer();
      while (matcher.find()) {
         final String group = matcher.group();
         check.append("," + group);
         list.add(group);
      }
      if (!spec.equals(check.toString().replaceFirst(",", ""))) {
        throw new BundleException("Syntax error specifying required-bundles list: '" + source + "'"); 
      }
    }
    return (String[])list.toArray(new String[0]);
  }

  public RequiredBundleSpec(final String spec) {
    attributes = new HashMap();
    final String[] data = spec.split(";");
    this.symbolicName = data[0];
    for (int i = 1; i < data.length; i++) {
      final String[] pairs = data[i].replaceAll(" ", "").split(":=");
      attributes.put(pairs[0], pairs[1]);
    }
  }

  public final String getSymbolicName() {
    return this.symbolicName;
  }

  public final String getBundleVersion() {
    final String bundleversion = (String) attributes.get(PROP_KEY_BUNDLE_VERSION);
    return (bundleversion == null) ? "(any-version)" : bundleversion;
  }

  public final boolean isOptional() {
    final String resolution = (String) attributes.get(PROP_KEY_RESOLUTION);
    return (resolution != null) && resolution.equals("optional");
  }

  public final boolean isCompatible(final String symbolicName, final String version) {
    // symbolic-names must match
    if (!this.symbolicName.equals(symbolicName)) { return false; }

    // if symbolic-names are matching, then check for version compatibility 
    String spec = (String) attributes.get(PROP_KEY_BUNDLE_VERSION);

    //  no specific bundle-version required/specified
    //  so it must be compatible with the version
    if (spec == null) { return true; }

    // clean up the version spec a bit
    spec = spec.replaceAll("\\\"", "");
    final VersionSpec target = new VersionSpec(version);

    // bundle-version:="1.0.0" 
    // anything >= 1.0.0
    if (!spec.startsWith("[") && !spec.startsWith("(")) {
      final VersionSpec v = new VersionSpec(spec);
      return (v.compareTo(target) <= 0);
    }

    // bundle-version:="[1.0.0, 1.0.1]"
    // anything within bounds of the given range
    final boolean inclusiveFloor = spec.startsWith("[");
    final boolean inclusiveCeiling = spec.endsWith("]");

    spec                      = spec.replaceAll("\\[|\\]|\\(|\\)", "");
    final String[] range      = spec.replaceAll(" ", "").split(",");
    final VersionSpec floor   = new VersionSpec(range[0]);
    final VersionSpec ceiling = new VersionSpec(range[1]);
    final boolean lowerBound  = inclusiveFloor ? (floor.compareTo(target) >= 0) : (floor.compareTo(target) > 0);
    final boolean upperBound  = inclusiveCeiling ? (target.compareTo(ceiling) <= 0) : (target.compareTo(ceiling) < 0);

    // it's compatible if version falls within the (lower|upper)-bound versions
    // according to the (in|ex)clusivity flags
    return (lowerBound && upperBound);
  }

  class VersionSpec {
    private String[] spec = new String[0];

    public VersionSpec(final String version) {
      this.spec = version.split("\\.");
    }

    public final String getMajor() {
      return spec[0];
    }

    public final String getMinor() {
      return spec.length > 1 ? spec[1] : "";
    }

    public final String getRevision() {
      return spec.length > 2 ? spec[2] : "";
    }

    public final String getBuild() {
      return spec.length > 3 ? spec[3] : "";
    }

    public final int compareTo(VersionSpec v) {
      int result = getMajor().compareTo(v.getMajor());
      if (result == 0) {
        result = getMinor().compareTo(v.getMinor());
        if (result == 0) {
          result = getRevision().compareTo(v.getRevision());
          if (result == 0) {
            result = getBuild().compareTo(v.getBuild());
          }
        }
      }
      return result;
    }

    public final String toString() {
      StringBuffer buffer = new StringBuffer(this.spec[0]);
      for (int i = 1; i < this.spec.length; i++) {
        buffer.append(".").append(this.spec[i]);
      }
      return buffer.toString();
    }
  }
}
