/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.license;

import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;

public class LicenseCheck {
  private static final TCLogger     consoleLogger  = CustomerLogging.getConsoleLogger();
  private static final Capabilities capabilities   = AbstractLicenseResolverFactory.getCapabilities();
  public static final String        EXIT_MESSAGE   = "TERRACOTTA IS EXITING. Contact your Terracotta sales representative to "
                                                     + "learn how to enable licensed usage of this feature. For more information, "
                                                     + "visit Terracotta sales at http://www.terracottatech.com.";
  public static final String        EXPIRY_WARNING = "Your product key is valid until %s. "
                                                     + "You have %s remaining until the expiration date. "
                                                     + "When the expiration date is reached TERRACOTTA WILL CEASE FUNCTIONING.";
  public static final String        EXPIRED_ERROR  = "Your product key expired on %s. " + EXIT_MESSAGE;
  public static final int           WARNING_MARK   = 240;
  public static final long          HOUR           = 1000 * 60 * 60;

  public static void checkCapability(Capability capability) {
    if (!capabilities.isSupported(capability)) {
      consoleLogger.error("Feature '" + capability + "' is not supported in this edition of Terracotta. "
                          + EXIT_MESSAGE);
      System.exit(1);
    }
    if (!capabilities.isLicensed(capability)) {
      consoleLogger.error("Your product key is not valid for the following requested feature '" + capability + "'. "
                          + EXIT_MESSAGE);
      System.exit(2);
    }
  }

}
