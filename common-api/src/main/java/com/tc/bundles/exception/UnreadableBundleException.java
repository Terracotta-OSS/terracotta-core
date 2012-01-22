/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.bundles.exception;

import org.osgi.framework.BundleException;

import com.tc.bundles.ResolverUtils;

import java.net.URL;

public class UnreadableBundleException extends BundleException implements BundleExceptionSummary {

  private URL bundle;

  public UnreadableBundleException(final String msg) {
    super(msg);
  }

  public UnreadableBundleException(final String msg, final Throwable cause) {
    super(msg, cause);
  }

  public UnreadableBundleException(final String msg, final URL bundle) {
    super(msg);
    this.bundle = bundle;
  }

  private String expectedLocation() {
    return ResolverUtils.canonicalize(bundle);
  }

  public String getSummary() {
    String msg = getMessage();
    msg += "\n\n" + INDENT + "TIM jar: " + bundle;
    msg += "\n\n" + INDENT + "Path to jar file: " + expectedLocation();
    return msg;
  }

}
