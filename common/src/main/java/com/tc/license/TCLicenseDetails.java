/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.license;

import org.apache.commons.io.IOUtils;
import org.terracotta.license.AbstractLicenseResolverFactory;
import org.terracotta.license.License;
import org.terracotta.license.LicenseConstants;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Set;

/**
 * A util class for SAG, see DEV-7279
 * 
 * @author hhuynh
 */
public class TCLicenseDetails {
  private License license;

  /**
   * Construct a TC license details. License file will be verified. LicenseException will be thrown if license check
   * fails
   */
  public TCLicenseDetails(File licenseFile) {
    InputStream input = null;
    try {
      input = new FileInputStream(licenseFile);
      license = AbstractLicenseResolverFactory.getFactory().resolveLicense(input);
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    } finally {
      IOUtils.closeQuietly(input);
    }
  }

  /**
   * returns the license object. All license info can be queried via this object
   */
  public License getLicense() {
    return license;
  }

  public boolean supportsL2Offheap() {
    return license.isCapabilityEnabled(LicenseConstants.CAPABILITY_TERRACOTTA_SERVER_ARRAY_OFFHEAP);
  }

  public boolean supportsL1Offheap() {
    return license.isCapabilityEnabled(LicenseConstants.CAPABILITY_EHCACHE_OFFHEAP);
  }

  /**
   * returns max amount L2 offheap. ie: 200G
   */
  public String maxL2Offheap() {
    return license.getRequiredProperty(LicenseConstants.TERRACOTTA_SERVER_ARRAY_MAX_OFFHEAP);
  }

  /**
   * returns max amount of L1 (ehcache) offheap. ie.: 200G
   */
  public String maxL1Offheap() {
    return license.getRequiredProperty(LicenseConstants.EHCACHE_MAX_OFFHEAP);
  }

  /**
   * capabilities allowed by this license. See {@link LicenseConstants} for possible values
   */
  public Set<String> licensedCapabilities() {
    return license.capabilities();
  }
}
