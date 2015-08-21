/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.license;

import static org.terracotta.license.LicenseConstants.CAPABILITY_AUTHENTICATION;
import static org.terracotta.license.LicenseConstants.CAPABILITY_SEARCH;
import static org.terracotta.license.LicenseConstants.CAPABILITY_SECURITY;
import static org.terracotta.license.LicenseConstants.CAPABILITY_SERVER_STRIPING;
import static org.terracotta.license.LicenseConstants.CAPABILITY_SESSIONS;
import static org.terracotta.license.LicenseConstants.CAPABILITY_TERRACOTTA_SERVER_ARRAY_OFFHEAP;
import static org.terracotta.license.LicenseConstants.LICENSE_CAPABILITIES;
import static org.terracotta.license.LicenseConstants.LICENSE_KEY_FILENAME;
import static org.terracotta.license.LicenseConstants.LICENSE_TYPE_TRIAL;
import static org.terracotta.license.LicenseConstants.PRODUCTKEY_PATH_PROPERTY;

import org.terracotta.license.AbstractLicenseResolverFactory;
import org.terracotta.license.License;
import org.terracotta.license.LicenseException;
import org.terracotta.license.util.MemorySizeParser;

import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.util.ProductInfo;

import java.io.InputStream;
import java.util.Date;

public class LicenseManager {

  private static final TCLogger                       CONSOLE_LOGGER = CustomerLogging.getConsoleLogger();
  public static final String                          ERROR_MESSAGE  = "Contact your Terracotta sales representative to "
                                                                       + "learn how to enable licensed usage of this feature. For more information, "
                                                                       + "visit Terracotta support at http://www.terracotta.org.";
  public static final String                          EXPIRY_WARNING         = "Your license key is valid until %s. "
                                                                       + "You have %s remaining until the expiration date. "
                                                                       + "When the expiration date is reached TERRACOTTA WILL CEASE FUNCTIONING.";
  public static final String                          EXPIRED_ERROR  = "Your product key expired on %s. "
                                                                       + ERROR_MESSAGE;
  public static final int                             WARNING_MARK   = 240;
  public static final long                            HOUR           = 1000 * 60 * 60;
  private static boolean                              initialized;
  private static final AbstractLicenseResolverFactory factory        = AbstractLicenseResolverFactory.getFactory();

  // lazily initialized, don't use it directly use getLicense() instead
  private static License                              license;


  private static synchronized void init() {
    license = factory.resolveLicense();
    afterInit(factory.getLicenseLocation());
  }

  public static synchronized void loadLicenseFromStream(InputStream in, String licenseLocation) {
    license = factory.resolveLicense(in);
    afterInit(licenseLocation);
  }

  private static void afterInit(String licenseLocation) {
    initialized = true;
    if (license != null) {
      CONSOLE_LOGGER.info("Terracotta license loaded from " + licenseLocation + "\n" + license.toString());
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
    String msg;
    if (getLicense() == null) {
      //
      CONSOLE_LOGGER
          .error("Terracotta license key is required for Enterprise capabilities. Please place "
                 + LICENSE_KEY_FILENAME
                 + " in the Terracotta installation directory or in the resource path. You can also specify it as a system property with -D"
                 + PRODUCTKEY_PATH_PROPERTY + "=/path/to/key");
      msg = "License key not found";
      CONSOLE_LOGGER.error(msg);
      throw new LicenseException(msg);
    }
    Date expirationDate = getLicense().expirationDate();
    if (expirationDate != null && expirationDate.before(new Date())) {
      msg = "Your Terracotta license has expired on " + expirationDate;
      CONSOLE_LOGGER.error(msg);
      throw new LicenseException(msg);
    }
  }

  public static void verifyCapability(String capability) {
    assertLicenseValid();
    if (!getLicense().isCapabilityEnabled(capability)) {
      //
      throw new LicenseException("Your license key doesn't allow usage of '" + capability + "' capability");
    }
  }

  public static void verifyAuthenticationCapability() {
    verifyCapability(CAPABILITY_AUTHENTICATION);
  }

  public static void verifyServerStripingCapability() {
    verifyCapability(CAPABILITY_SERVER_STRIPING);
  }

  public static void verifySessionCapability() {
    verifyCapability(CAPABILITY_SESSIONS);
  }

  public static int maxClientCount() {
    assertLicenseValid();
    return getLicense().maxClientCount();
  }

  public static String licensedCapabilities() {
    assertLicenseValid();
    return getLicense().getRequiredProperty(LICENSE_CAPABILITIES);
  }

  public static void verifyServerArrayOffheapCapability(String maxDataStorageConfigured) {
    verifyCapability(CAPABILITY_TERRACOTTA_SERVER_ARRAY_OFFHEAP);


    String maxDataStorageSizeFromLicense = getLicense().maxServerArrayDataStorage();
    long maxDataStorageLicensedInBytes = MemorySizeParser.parse(maxDataStorageSizeFromLicense);
    long maxDataStroageConfiguredInBytes = MemorySizeParser.parse(maxDataStorageConfigured);

    if (CONSOLE_LOGGER.isDebugEnabled()) {
      CONSOLE_LOGGER.debug("max dataStorage size licensed: " + maxDataStorageLicensedInBytes);
      CONSOLE_LOGGER.debug("max dataStorage size configured: " + maxDataStroageConfiguredInBytes);
    }

    boolean dataStorageSizeAllowed = maxDataStroageConfiguredInBytes <= maxDataStorageLicensedInBytes;
    if (!dataStorageSizeAllowed) {
      throw new LicenseException("Your license only allows up to " + maxDataStorageSizeFromLicense
                                 + " in dataStorage size. Your Terracotta server is configured with "
                                 + maxDataStorageConfigured);
    }
  }

  public static boolean enterpriseEdition() {
    return ProductInfo.getInstance().isEnterprise();
  }

  public static boolean isTrialLicense() {
    assertLicenseValid();
    return getLicense().type().equals(LICENSE_TYPE_TRIAL);
  }

  public static void verifySearchCapability() {
    verifyCapability(CAPABILITY_SEARCH);
  }

  public static void verifySecurityCapability() {
    verifyCapability(CAPABILITY_SECURITY);
  }
}
