/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.bundles;

import com.tc.util.version.VersionRange;

import java.util.HashMap;
import java.util.Map;

/**
 * Specification for the Require-Bundle attribute
 * 
 * <pre>
 * SYNTAX:
 * Require-Bundle ::= bundle {, bundle...}
 * bundle ::= symbolic-name{;bundle-version:=&quot;constraint&quot;{;resolution:=optional}}
 * constraint ::= [range] || (range)
 * range ::= min, {max}
 * 
 * EXAMPLES:
 * Require-Bundle: foo.bar.baz.widget - require widget bundle from group foo.bar.baz
 * Require-Bundle: foo.bar.baz.widget, foo.bar.baz.gadget - require widget and gadget bundles
 * Require-Bundle: foo.bar.baz.widget;bundle-version:=&quot;1.0.0&quot; - widget bundle must be version 1.0.0
 * Require-Bundle: foo.bar.baz.widget;bundle-version:=&quot;[1.0.0, 2.0.0]&quot; - bundle version must &gt; 1.0.0 and &lt; 2.0.0
 * Require-Bundle: foo.bar.baz.widget;bundle-version:=&quot;[1.0.0, 2.0.0)&quot; - bundle version must &gt; 1.0.0 and &lt;= 2.0.0
 * Require-Bundle: foo.bar.baz.widget;bundle-version:=&quot;(1.0.0, 2.0.0)&quot; - bundle version must &gt;= 1.0.0 and &lt;= 2.0.0
 * Require-Bundle: foo.bar.baz.widget;bundle-version:=&quot;(1.0.0, 2.0.0]&quot; - bundle version must &gt;= 1.0.0 and &lt; 2.0.0
 * Require-Bundle: foo.bar.baz.widget;bundle-version:=&quot;[1.0.0,]&quot; - bundle version must &gt; 1.0.0
 * Require-Bundle: foo.bar.baz.widget;bundle-version:=&quot;(1.0.0,)&quot; - bundle version must &gt;= 1.0.0
 * Require-Bundle: foo.bar.baz.widget;resolution:=optional - bundle is optional (recognized but not supported)
 * </pre>
 */
final class BundleSpecImpl extends BundleSpec {
  private final String symbolicName;
  private final Map    attributes = new HashMap();

  BundleSpecImpl(final String spec) {
    final String[] data = spec.split(";");
    this.symbolicName = data[0];
    for (int i = 1; i < data.length; i++) {
      final String[] pairs = data[i].replaceAll(" ", "").split(":=");
      attributes.put(pairs[0], pairs[1].replaceAll("\\\"", ""));
    }
  }

  @Override
  public String getSymbolicName() {
    return this.symbolicName;
  }

  @Override
  public String getName() {
    return extractInfo("name");
  }

  @Override
  public String getGroupId() {
    return extractInfo("group-id");
  }

  private String extractInfo(final String n) {
    final String[] pieces = this.symbolicName.split("\\.");
    int k = 0;
    for (int i = pieces.length - 1; i >= 0; i--) {
      if (pieces[i].matches("^" + BUNDLE_SYMBOLIC_NAME_REGEX)) {
        k = i;
        break;
      }
    }
    final int start = "name".equals(n) ? k : 0;
    final int end = "name".equals(n) ? pieces.length : k;
    final StringBuffer result = new StringBuffer();
    for (int j = start; j < end; j++) {
      result.append(pieces[j]).append(".");
    }
    return result.toString().replaceFirst("\\.$", "");
  }

  @Override
  public String getVersion() {
    return isVersionSpecified() ? getVersionSpec() : "(any-version)";
  }

  @Override
  public boolean isOptional() {
    final String resolution = (String) attributes.get(PROP_KEY_RESOLUTION);
    return (resolution != null) && resolution.equals("optional");
  }

  @Override
  public boolean isVersionSpecified() {
    return getVersionSpec().length() > 0;
  }

  @Override
  public boolean isVersionSpecifiedAbsolute() {
    return getVersionSpec().matches(IConstants.OSGI_VERSION_PATTERN.pattern());
  }

  private String getVersionSpec() {
    final String verspec = (String) attributes.get(PROP_KEY_BUNDLE_VERSION);
    return (verspec == null) ? "" : verspec;
  }

  private String getMavenVersion() {
    String mavenVersion = getVersionSpec();

    mavenVersion = OSGiToMaven.mavenVersionFromOsgiVersion(mavenVersion);

    return mavenVersion;
  }

  @Override
  public boolean isCompatible(final String symName, final String version) {
    // symbolic-names must match
    if (!BundleSpecUtil.isMatchingSymbolicName(symbolicName, symName)) return false;

    // if symbolic-names are matching, then check for version compatibility -
    // and if no specific bundle-version required/specified so it must be compatible with the version
    if (!isVersionSpecified()) return true;

    // otherwise check if the version is within range of the specified required version
    VersionRange range = new VersionRange(getMavenVersion());
    return range.contains(OSGiToMaven.bundleVersionToProjectVersion(version));
  }
}
