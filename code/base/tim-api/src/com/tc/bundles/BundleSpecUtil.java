/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.bundles;

public class BundleSpecUtil {
  public final static boolean isMatchingSymbolicName(final String arg0, final String arg1) {
    return (arg0 != null) && (arg1 != null) && arg0.equalsIgnoreCase(arg1);
  }
}
