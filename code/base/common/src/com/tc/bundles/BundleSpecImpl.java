/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.bundles;

import org.knopflerfish.framework.VersionRange;

import com.tc.bundles.Version;

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
public final class BundleSpecImpl extends BundleSpec {
  private final String symbolicName;
  private final Map    attributes = new HashMap();

  public BundleSpecImpl(final String spec) {
    final String[] data = spec.split(";");
    this.symbolicName = data[0];
    for (int i = 1; i < data.length; i++) {
      final String[] pairs = data[i].replaceAll(" ", "").split(":=");
      attributes.put(pairs[0], pairs[1].replaceAll("\\\"", ""));
    }
  }

  public String getSymbolicName() {
    return this.symbolicName;
  }

  public String getName() {
    return extractInfo("name");
  }

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

  public String getVersion() {
    final String bundleversion = (String) attributes.get(PROP_KEY_BUNDLE_VERSION);
    return (bundleversion == null) ? "(any-version)" : bundleversion;
  }

  public boolean isOptional() {
    final String resolution = (String) attributes.get(PROP_KEY_RESOLUTION);
    return (resolution != null) && resolution.equals("optional");
  }

  public boolean isCompatible(final String symname, final String version) {
    // symbolic-names must match
    if (!BundleSpec.isMatchingSymbolicName(this.symbolicName, symname)) return false;

    // if symbolic-names are matching, then check for version compatibility
    String spec = (String) attributes.get(PROP_KEY_BUNDLE_VERSION);

    // no specific bundle-version required/specified
    // so it must be compatible with the version
    if (spec == null) return true;

    final Version target = new Version(version);
    VersionRange range = new VersionRange(spec);

    return range.withinRange(target);
  }
}
