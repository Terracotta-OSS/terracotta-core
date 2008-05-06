/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.bundles;

import java.util.ArrayList;
import java.util.List;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.lang.reflect.Constructor;
import java.text.MessageFormat;

public abstract class BundleSpec {

  private static final String   REQUIRE_BUNDLE              = "Require-Bundle";
  protected static final String PROP_KEY_RESOLUTION         = "resolution";
  protected static final String PROP_KEY_BUNDLE_VERSION     = "bundle-version";
  protected static final String BUNDLE_SYMBOLIC_NAME_REGEX  = "[a-zA-Z][A-Za-z0-9._\\-]+";
  protected static final String REQUIRE_BUNDLE_EXPR_MATCHER = "(" + BUNDLE_SYMBOLIC_NAME_REGEX
                                                              + "(;resolution:=\"?optional\"?)?" + //
                                                              "(;bundle-version:=(\"?[A-Za-z0-9.]+\"?|" + //
                                                              "\"?[\\[\\(][A-Za-z0-9.]+,[A-Za-z0-9.]*[\\]\\)]\"?))?)";

  public abstract String getSymbolicName();

  public abstract String getName();

  public abstract String getGroupId();

  public abstract String getVersion();

  public abstract boolean isOptional();

  public abstract boolean isCompatible(final String symname, final String version);

  public abstract boolean isVersionSpecified();

  public abstract boolean isVersionSpecifiedAbsolute();
  
  public final static boolean isMatchingSymbolicName(final String arg0, final String arg1) {
    return (arg0 != null) && (arg1 != null) && arg0.equalsIgnoreCase(arg1);
  }

  public static final String getSymbolicName(final Manifest manifest) {
    return manifest.getMainAttributes().getValue("Bundle-SymbolicName");
  }
  
  public static final String getName(final Manifest manifest) {
    return manifest.getMainAttributes().getValue("Bundle-Name");
  }

  public static final String getVersion(final Manifest manifest) {
    return manifest.getMainAttributes().getValue("Bundle-Version");
  }
  
  public static final String getDescription(final Manifest manifest) {
    return manifest.getMainAttributes().getValue("Bundle-Description");
  }
  
  public static final String[] getRequirements(final Manifest manifest) {
    return getRequirements(manifest.getMainAttributes().getValue(REQUIRE_BUNDLE));
  }

  public static final String[] getRequirements(final String requiredBundles) {
    if (requiredBundles == null) return new String[0];

    final List list = new ArrayList();
    final String spec = requiredBundles.replaceAll(" ", "");
    final Pattern pattern = Pattern.compile(REQUIRE_BUNDLE_EXPR_MATCHER);
    final Matcher matcher = pattern.matcher(spec);
    final StringBuffer check = new StringBuffer();

    while (matcher.find()) {
      final String group = matcher.group();
      check.append("," + group);
      list.add(group);
    }

    if (!spec.equals(check.toString().replaceFirst(",", ""))) {
      final String arg0 = "Syntax error specifying the required bundle list in the manifest: ''{0}'' found ''{1}''";
      final Object[] arg1 = { requiredBundles, check };
      throw new RuntimeException(MessageFormat.format(arg0, arg1));
    }

    return (String[]) list.toArray(new String[list.size()]);
  }

  public static final BundleSpec newInstance(final String spec) {
    final String BUNDLESPECIMPL = "com.tc.bundles.BundleSpecImpl";
    try {
      final Class klass = Class.forName(BUNDLESPECIMPL);
      final Constructor constructor = klass.getDeclaredConstructor(new Class[] { String.class });
      return (BundleSpec) constructor.newInstance(new Object[] { spec });
    } catch (Exception e) {
      throw new RuntimeException("Unable to create an instance of class " + BUNDLESPECIMPL, e);
    }
  }
}