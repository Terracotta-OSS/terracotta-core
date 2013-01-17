/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.feature;

import org.terracotta.toolkit.internal.feature.LicenseFeature;

public class NoopLicenseFeature extends EnabledToolkitFeature implements LicenseFeature {

  public static final NoopLicenseFeature SINGLETON = new NoopLicenseFeature();

  private NoopLicenseFeature() {
    // private
  }

  @Override
  public boolean isLicenseEnabled(String licenseName) {
    return false;
  }

}
