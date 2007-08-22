/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.bundles.exception;

import org.osgi.framework.BundleException;

public class InvalidBundleManifestException extends BundleException {

  public InvalidBundleManifestException(final String msg) {
    super(msg);
  }

  public InvalidBundleManifestException(final String msg, final Throwable cause) {
    super(msg, cause);
  }

}
