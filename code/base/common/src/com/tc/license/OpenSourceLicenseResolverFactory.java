/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.license;

public class OpenSourceLicenseResolverFactory extends AbstractLicenseResolverFactory {
  private final License license;

  public OpenSourceLicenseResolverFactory() {
    license = LicenseFactory.createOpenSourceLicense();
  }

  public License resolveLicense() {
    return license;
  }
}
