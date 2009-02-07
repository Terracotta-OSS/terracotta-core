/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.license;

import com.tc.util.factory.AbstractFactory;

public abstract class AbstractLicenseResolverFactory extends AbstractFactory {
  private static final String NEWLINE                           = System.getProperty("line.separator");
  private static String       FACTORY_SERVICE_ID                = "com.tc.license.LicenseResolverFactory";
  private static Class        OPENSOURCE_RESOLVER_FACTORY_CLASS = OpenSourceLicenseResolverFactory.class;

  private static License      license;

  public static AbstractLicenseResolverFactory getFactory() {
    return (AbstractLicenseResolverFactory) getFactory(FACTORY_SERVICE_ID, OPENSOURCE_RESOLVER_FACTORY_CLASS);
  }

  public abstract License resolveLicense();

  public static synchronized License getLicense() {
    if (license == null) {
      license = getFactory().resolveLicense();
    }
    return license;
  }

  public static Capabilities getCapabilities() {
    return getLicense().capabilities();
  }

  public static String getLicenseWarning(String feature) {
    String message = NEWLINE + "---------------- LICENSE VIOLATION WARNING --------------------";
    message += NEWLINE + "Your Terracotta license doesn't have " + feature + " capability.";
    message += NEWLINE + "Please remove it from your configuration." + NEWLINE;
    return message;
  }

  public static String getLicenseWarning(String feature, String offendingItem) {
    String message = getLicenseWarning(feature);
    message += "Offending item: " + offendingItem + NEWLINE;
    return message;
  }
}
