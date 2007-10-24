/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.bundles;

public abstract class BundleSpec {

  public abstract String getSymbolicName();

  public abstract String getName();

  public abstract String getGroupId();

  public abstract String getVersion();

  public abstract boolean isOptional();

  public abstract boolean isCompatible(final String symname, final String version);

  public final static boolean isMatchingSymbolicName(final String arg0, final String arg1) {
    return arg0.replace('-', '_').equalsIgnoreCase(arg1.replace('-', '_'));
  }
}