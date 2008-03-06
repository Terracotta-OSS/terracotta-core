/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.bundles.exception;

import org.osgi.framework.BundleException;

import com.tc.bundles.BundleSpec;

public class BundleSpecException extends BundleException {

  public BundleSpecException(final String msg) {
    super(msg);
  }

  public static BundleSpecException unspecifiedVersion(final BundleSpec spec) {
    String msg = "Incomplete bundle spec, the version number must also be supplied in order to locate "
                 + "the bundle named '" + spec.getName() + "'";
    if (spec.getGroupId().length() > 0) msg += " (group-id: " + spec.getGroupId() + ")";
    return new BundleSpecException.UnspecifiedVersion(msg);
  }

  static class UnspecifiedVersion extends BundleSpecException {

    public UnspecifiedVersion(String msg) {
      super(msg);
    }

  }
}
