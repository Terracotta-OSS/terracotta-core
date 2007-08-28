/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.bundles;

import org.osgi.framework.BundleException;

import com.tc.bundles.exception.InvalidBundleManifestException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Specification for the Require-Bundle attribute
 *  
 * <pre>
 * SYNTAX:
 * Require-Bundle ::= bundle {, bundle...}
 * bundle ::= symbolic-name{;bundle-version:="constraint"{;resolution:=optional}}
 * constraint ::= [range] || (range)
 * range ::= min, {max}
 * 
 * EXAMPLES:
 * Require-Bundle: foo.bar.baz.widget - require widget bundle from group foo.bar.baz
 * Require-Bundle: foo.bar.baz.widget, foo.bar.baz.gadget - require widget and gadget bundles
 * Require-Bundle: foo.bar.baz.widget;bundle-version:="1.0.0" - widget bundle must be version 1.0.0
 * Require-Bundle: foo.bar.baz.widget;bundle-version:="[1.0.0, 2.0.0]" - bundle version must > 1.0.0 and < 2.0.0
 * Require-Bundle: foo.bar.baz.widget;bundle-version:="[1.0.0, 2.0.0)" - bundle version must > 1.0.0 and <= 2.0.0
 * Require-Bundle: foo.bar.baz.widget;bundle-version:="(1.0.0, 2.0.0)" - bundle version must >= 1.0.0 and <= 2.0.0
 * Require-Bundle: foo.bar.baz.widget;bundle-version:="(1.0.0, 2.0.0]" - bundle version must >= 1.0.0 and < 2.0.0
 * Require-Bundle: foo.bar.baz.widget;bundle-version:="[1.0.0,]" - bundle version must > 1.0.0
 * Require-Bundle: foo.bar.baz.widget;bundle-version:="(1.0.0,)" - bundle version must >= 1.0.0
 * Require-Bundle: foo.bar.baz.widget;resolution:=optional - bundle is optional (recognized but not supported)
 * </pre>
 */
public final class BundleSpec {
  private static final String PROP_KEY_RESOLUTION         = "resolution";
  private static final String PROP_KEY_BUNDLE_VERSION     = "bundle-version";
  private static final String REQUIRE_BUNDLE_EXPR_MATCHER = "([A-Za-z0-9._\\-]+(;resolution:=\"optional\")?(;bundle-version:=(\"[A-Za-z0-9.]+\"|\"\\[[A-Za-z0-9.]+,[A-Za-z0-9.]+\\]\"))?)";

  private final String        symbolicName;
  private Map                 attributes;

  public static final String[] getRequirements(final Manifest manifest) throws BundleException {
    return getRequirements(manifest.getMainAttributes().getValue("Require-Bundle"));
  }

  public static final String[] getRequirements(final String source) throws BundleException {
    if (source == null) return new String[0];

    final List list = new ArrayList();
    final String spec = source.replaceAll(" ", "");
    final Pattern pattern = Pattern.compile(REQUIRE_BUNDLE_EXPR_MATCHER);
    final Matcher matcher = pattern.matcher(spec);
    final StringBuffer check = new StringBuffer();

    while (matcher.find()) {
      final String group = matcher.group();
      check.append("," + group);
      list.add(group);
    }

    if (!spec.equals(check.toString().replaceFirst(",", ""))) throw new InvalidBundleManifestException(
        "Syntax error specifying Require-Bundle: " + source);

    return (String[]) list.toArray(new String[0]);
  }

  public static final String[] parseList(final String source) throws BundleException {
    return getRequirements(source);
  }

  public BundleSpec(final String spec) {
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

  public final String getName() {
    return extractInfo("name");
  }

  public final String getGroupId() {
    return extractInfo("group-id");
  }

  private final String extractInfo(final String n) {
    final String[] pieces = this.symbolicName.split("\\.");
    int k = 0;
    for (int i = pieces.length - 1; i >= 0; i--) {
      if (pieces[i].matches("^[a-zA-Z][a-zA-Z0-9_]+")) {
        k = i;
        break;
      }
    }
    final StringBuffer result = new StringBuffer();
    final int start = n.equals("name") ? k : 0;
    final int end = n.equals("name") ? pieces.length : k;
    for (int j = start; j < end; j++) {
      result.append(pieces[j]).append(".");
    }
    return result.toString().replaceFirst("\\.$", "");
  }

  public final String getVersion() {
    final String bundleversion = (String) attributes.get(PROP_KEY_BUNDLE_VERSION);
    return (bundleversion == null) ? "(any-version)" : bundleversion;
  }

  public final boolean isOptional() {
    final String resolution = (String) attributes.get(PROP_KEY_RESOLUTION);
    return (resolution != null) && resolution.equals("optional");
  }

  public final boolean isCompatible(final String symname, final String version) {
    // symbolic-names must match
    if (!this.symbolicName.equals(symname)) { return false; }

    // if symbolic-names are matching, then check for version compatibility
    String spec = (String) attributes.get(PROP_KEY_BUNDLE_VERSION);

    // no specific bundle-version required/specified
    // so it must be compatible with the version
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

    spec = spec.replaceAll("\\[|\\]|\\(|\\)", "");
    final String[] range = spec.replaceAll(" ", "").split(",");
    final VersionSpec floor = new VersionSpec(range[0]);
    final VersionSpec ceiling = new VersionSpec(range[1]);
    final boolean lowerBound = inclusiveFloor ? (floor.compareTo(target) >= 0) : (floor.compareTo(target) > 0);
    final boolean upperBound = inclusiveCeiling ? (target.compareTo(ceiling) <= 0) : (target.compareTo(ceiling) < 0);

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
