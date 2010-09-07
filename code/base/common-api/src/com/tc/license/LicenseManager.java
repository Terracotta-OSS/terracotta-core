/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.license;

import static org.terracotta.license.LicenseConstants.CAPABILITY_AUTHENTICATION;
import static org.terracotta.license.LicenseConstants.CAPABILITY_DCV2;
import static org.terracotta.license.LicenseConstants.CAPABILITY_OPERATOR_CONSOLE;
import static org.terracotta.license.LicenseConstants.CAPABILITY_ROOTS;
import static org.terracotta.license.LicenseConstants.CAPABILITY_SERVER_STRIPING;
import static org.terracotta.license.LicenseConstants.CAPABILITY_SESSIONS;
import static org.terracotta.license.LicenseConstants.CAPABILITY_TERRACOTTA_SERVER_ARRAY_OFFHEAP;
import static org.terracotta.license.LicenseConstants.EHCACHE_MAX_OFFHEAP;
import static org.terracotta.license.LicenseConstants.LICENSE_CAPABILITIES;
import static org.terracotta.license.LicenseConstants.TERRACOTTA_MAX_CLIENT_COUNT;

import org.terracotta.license.AbstractLicenseResolverFactory;
import org.terracotta.license.License;
import org.terracotta.license.LicenseException;
import org.terracotta.license.util.MemorySizeParser;

import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.util.runtime.Vm;

import java.util.Date;

public class LicenseManager {
  private static final long       BYTES_PER_MEGABYTE = 1024 * 1024L;
  private static final long       BYTES_PER_GIGABYTE = 1024 * 1024L * 1024L;
  private static final TCLogger   LOGGER             = CustomerLogging.getConsoleLogger();
  public static final String      EXIT_MESSAGE       = "TERRACOTTA IS EXITING. Contact your Terracotta sales representative to "
                                                       + "learn how to enable licensed usage of this feature. For more information, "
                                                       + "visit Terracotta support at http://www.terracotta.org.";
  public static final String      EXPIRY_WARNING     = "Your product key is valid until %s. "
                                                       + "You have %s remaining until the expiration date. "
                                                       + "When the expiration date is reached TERRACOTTA WILL CEASE FUNCTIONING.";
  public static final String      EXPIRED_ERROR      = "Your product key expired on %s. " + EXIT_MESSAGE;
  public static final int         WARNING_MARK       = 240;
  public static final long        HOUR               = 1000 * 60 * 60;

  private static volatile boolean initialized;

  // lazily-init, don't use directly
  // use getLicense() instead
  private static License          license;

  private static synchronized void init() {
    AbstractLicenseResolverFactory factory = AbstractLicenseResolverFactory.getFactory();
    license = factory.resolveLicense();
    initialized = true;
    if (license != null) {
      LOGGER.info("Terracotta license loaded from " + factory.getLicenseLocation() + "\n" + license.toString());
    }
  }

  public static synchronized License getLicense() {
    if (!initialized) {
      init();
    }
    return license;
  }

  /**
   * check for null and expired license
   */
  public static void assertLicenseValid() {
    if (getLicense() == null) { throw new LicenseException(
                                                           "Terracotta license key is required for Enterprise feature. The license key could be placed in the root of resource path."); }
    Date expirationDate = getLicense().expirationDate();
    if (expirationDate != null && expirationDate.before(new Date())) { throw new LicenseException(
                                                                                                  "Your Terracotta license has expired on "
                                                                                                      + expirationDate); }
  }

  private static void verifyCapability(String capability) {
    assertLicenseValid();
    if (!getLicense().isCapabilityEnabled(capability)) {
      //
      throw new LicenseException("Your license key doesn't allow usage of '" + capability + "' capability");
    }
  }

  public static void verifyOperatorConsoleCapability() {
    verifyCapability(CAPABILITY_OPERATOR_CONSOLE);
  }

  public static void verifyAuthenticationCapability() {
    verifyCapability(CAPABILITY_AUTHENTICATION);
  }

  public static void verifyServerStripingCapability() {
    verifyCapability(CAPABILITY_SERVER_STRIPING);
  }

  public static void verifyDCV2Capability() {
    verifyCapability(CAPABILITY_DCV2);
  }

  public static void verifyRootCapability() {
    verifyCapability(CAPABILITY_ROOTS);
  }

  public static void verifySessionCapability() {
    verifyCapability(CAPABILITY_SESSIONS);
  }

  public static int maxClientCount() {
    assertLicenseValid();
    String maxClientCount = getLicense().getRequiredProperty(TERRACOTTA_MAX_CLIENT_COUNT);
    return Integer.valueOf(maxClientCount);
  }

  public static String licensedCapabilities() {
    assertLicenseValid();
    return getLicense().getRequiredProperty(LICENSE_CAPABILITIES);
  }

  public static void verifyServerArrayOffheapCapability(String maxOffheap) {
    assertLicenseValid();
    verifyCapability(CAPABILITY_TERRACOTTA_SERVER_ARRAY_OFFHEAP);
    long maxHeapFromVMInBytes = Vm.maxDirectMemory();
    if (maxHeapFromVMInBytes == 0 || maxHeapFromVMInBytes == Long.MAX_VALUE) { throw new LicenseException(
                                                                                                          "No direct memory was set at JVM level. Please set it with -XX:MaxDirectMemorySize"); }

    String maxHeapSizeFromLicense = getLicense().getRequiredProperty(EHCACHE_MAX_OFFHEAP);
    long maxHeapAllowedInBytes = MemorySizeParser.parse(maxHeapSizeFromLicense);

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("max offheap from VM: " + maxHeapFromVMInBytes);
      LOGGER.debug("max offheap allowed: " + maxHeapAllowedInBytes);
    }

    if (maxHeapFromVMInBytes > maxHeapAllowedInBytes) {
      long maxHeapFromVM = maxHeapFromVMInBytes / BYTES_PER_GIGABYTE;
      String unit = "GB";
      if (maxHeapFromVM == 0) {
        maxHeapFromVM = maxHeapFromVMInBytes / BYTES_PER_MEGABYTE;
        unit = "MB";
      }
      throw new LicenseException("Your license only allows up to " + maxHeapSizeFromLicense
                                 + " in offheap size. Your VM is configured with "
                                 + maxHeapFromVM + unit);
    }
  }
}
