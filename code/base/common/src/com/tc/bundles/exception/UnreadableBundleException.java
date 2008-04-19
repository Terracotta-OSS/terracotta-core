/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.bundles.exception;

import org.osgi.framework.BundleException;

import java.io.File;
import java.io.IOException;

public class UnreadableBundleException extends BundleException implements Hints {

  private File bundle;
  
  public UnreadableBundleException(final String msg) {
    super(msg);
  }

  public UnreadableBundleException(final String msg, final Throwable cause) {
    super(msg, cause);
  }
  
  public UnreadableBundleException(final String msg, final File bundle) {
    super(msg);
    this.bundle = bundle;
  }

  private String expectedJarFilename() {
    return bundle.getName();
  }

  private String expectedLocation() {
    try {
      return bundle.getParentFile().getCanonicalPath();
    } catch (IOException e) {
      return bundle.getParentFile().toString();
    }
  }

  public String getHints() {
    String msg = getMessage();
    msg += "\n\nTIM jar filename: " + expectedJarFilename();
    msg += "\n\nJar file location: " + expectedLocation();
    return msg;
  }
  
}
