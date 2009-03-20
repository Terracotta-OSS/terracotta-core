package com.tc.license;

import com.tc.license.util.LicenseConstants;

import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public final class EnterpriseLicense implements License {
  private static final String NEWLINE = System.getProperty("line.separator");
  private final String        licenseType;
  private final String        licenseNumber;
  private final String        licensee;
  private final String        product;
  private final int           maxClients;
  private final Date          expirationDate;
  private final Capabilities  capabilities;
  private String              signature;

  public EnterpriseLicense(String type, String number, String licensee, String product, int maxClients,
                           Date expirationDate, Capabilities capabilities) {
    this.licenseType = type;
    this.licenseNumber = number;
    this.licensee = licensee;
    this.product = product;
    this.maxClients = maxClients;
    this.capabilities = capabilities;
    if (expirationDate != null) {
      this.expirationDate = new Date(expirationDate.getTime());
    } else {
      this.expirationDate = null;
    }
  }

  public Date expirationDate() {
    return expirationDate != null ? new Date(expirationDate.getTime()) : null;
  }

  public String licenseNumber() {
    return licenseNumber;
  }

  public String licenseType() {
    return licenseType;
  }

  public String licensee() {
    return licensee;
  }

  public int maxClients() {
    return maxClients;
  }

  public String product() {
    return product;
  }

  public Capabilities capabilities() {
    return capabilities;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(LicenseConstants.LICENSE_TYPE).append(" = ").append(licenseType).append(NEWLINE);
    sb.append(LicenseConstants.LICENSE_NUMBER).append(" = ").append(licenseNumber).append(NEWLINE);
    sb.append(LicenseConstants.LICENSEE).append(" = ").append(licensee).append(NEWLINE);
    sb.append(LicenseConstants.PRODUCT).append(" = ").append(product).append(NEWLINE);
    sb.append(LicenseConstants.MAX_CLIENTS).append(" = ").append(maxClients).append(NEWLINE);
    sb.append(LicenseConstants.CAPABILITIES).append(" = ").append(capabilities.getLicensedCapabilitiesAsString())
        .append(NEWLINE);
    if (expirationDate != null) {
      sb.append(LicenseConstants.EXPIRATION_DATE).append(" = ").append(dateToString(expirationDate)).append(NEWLINE);
    }
    return sb.toString();
  }

  private static String dateToString(Date date) {
    DateFormat df = new SimpleDateFormat(LicenseConstants.DATE_FORMAT);
    return df.format(date);
  }

  public final byte[] getCanonicalData() {
    StringBuilder sb = new StringBuilder();
    sb.append(LicenseConstants.LICENSE_TYPE).append(licenseType);
    sb.append(LicenseConstants.LICENSE_NUMBER).append(licenseNumber);
    sb.append(LicenseConstants.LICENSEE).append(licensee);
    sb.append(LicenseConstants.PRODUCT).append(product);
    sb.append(LicenseConstants.MAX_CLIENTS).append(maxClients);
    sb.append(LicenseConstants.CAPABILITIES).append(capabilities.getLicensedCapabilitiesAsString());
    if (expirationDate != null) {
      sb.append(LicenseConstants.EXPIRATION_DATE).append(dateToString(expirationDate));
    }
    try {
      return sb.toString().getBytes(LicenseConstants.CANONICAL_ENCODING);
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  public String getSignature() {
    return signature;
  }

  public void setSignature(String signature) {
    this.signature = signature;
  }
}