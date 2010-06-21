package com.tc.license;

import com.tc.license.util.LicenseConstants;
import com.tc.license.util.LicenseException;

import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public final class EnterpriseLicense implements License {
  private static final String NEWLINE = System.getProperty("line.separator");
  private final String        licenseType;
  private final String        licenseNumber;
  private final String        licensee;
  private final String        product;
  private final String        edition;
  private final int           maxClients;
  private final Date          expirationDate;
  private final Capabilities  capabilities;
  private String              signature;

  public EnterpriseLicense(Map<String, Object> licenseFields, Capabilities capabilities) {
    this.licenseType = (String) licenseFields.get(LicenseConstants.LICENSE_TYPE);
    this.licenseNumber = (String) licenseFields.get(LicenseConstants.LICENSE_NUMBER);
    this.licensee = (String) licenseFields.get(LicenseConstants.LICENSEE);
    this.product = (String) licenseFields.get(LicenseConstants.PRODUCT);
    this.edition = (String) licenseFields.get(LicenseConstants.EDITION);
    this.maxClients = (Integer) licenseFields.get(LicenseConstants.MAX_CLIENTS);
    this.capabilities = capabilities;
    Date expireDate = (Date) licenseFields.get(LicenseConstants.EXPIRATION_DATE);
    this.expirationDate = expireDate != null ? new Date(expireDate.getTime()) : null;
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

  public String edition() {
    return edition;
  }

  public Capabilities capabilities() {
    return capabilities;
  }

  public String toString() {
    return asString(NEWLINE);
  }

  public byte[] getCanonicalData() {
    String rawData = asString("");
    try {
      return rawData.getBytes(LicenseConstants.CANONICAL_ENCODING);
    } catch (UnsupportedEncodingException e) {
      throw new LicenseException(e);
    }
  }

  private static String dateToString(Date date) {
    DateFormat df = new SimpleDateFormat(LicenseConstants.DATE_FORMAT);
    return df.format(date);
  }

  public String getSignature() {
    return signature;
  }

  public void setSignature(String signature) {
    this.signature = signature;
  }

  private String asString(String delimiter) {
    StringBuilder sb = new StringBuilder();
    sb.append(LicenseConstants.LICENSE_TYPE).append(" = ").append(licenseType).append(delimiter);
    sb.append(LicenseConstants.LICENSE_NUMBER).append(" = ").append(licenseNumber).append(delimiter);
    sb.append(LicenseConstants.LICENSEE).append(" = ").append(licensee).append(delimiter);
    sb.append(LicenseConstants.PRODUCT).append(" = ").append(product).append(delimiter);
    sb.append(LicenseConstants.EDITION).append(" = ").append(edition).append(delimiter);
    sb.append(LicenseConstants.MAX_CLIENTS).append(" = ").append(maxClients).append(delimiter);
    sb.append(LicenseConstants.CAPABILITIES).append(" = ").append(capabilities.getLicensedCapabilitiesAsString())
        .append(delimiter);
    if (expirationDate != null) {
      sb.append(LicenseConstants.EXPIRATION_DATE).append(" = ").append(dateToString(expirationDate)).append(delimiter);
    }
    return sb.toString();
  }
}