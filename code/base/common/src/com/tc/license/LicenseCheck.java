/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.license;

import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;

public class LicenseCheck {
  private static final TCLogger     consoleLogger = CustomerLogging.getConsoleLogger();
  private static final Capabilities capabilities  = AbstractLicenseResolverFactory.getCapabilities();

  public static void checkCapability(Capability capability) {
    if (!capabilities.isSupported(capability)) {
      consoleLogger.error("Feature '" + capability + "' is not supported in this edition of Terracotta.");
      System.exit(1);
    }
    if (!capabilities.isLicensed(capability)) {
      consoleLogger.error("Your product key is not valid for the following requested feature '" + capability
                          + "'. TERRACOTTA IS EXITING. Contact your Terracotta sales representative to "
                          + "learn how to enable licensed usage of this feature. For more information, "
                          + "visit Terracotta sales at http://www.terracottatech.com.");
      System.exit(2);
    }
  }
}
