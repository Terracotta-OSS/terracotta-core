/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.license.util;

import static com.tc.license.util.LicenseConstants.CUSTOM;
import static com.tc.license.util.LicenseConstants.EDITION;
import static com.tc.license.util.LicenseConstants.EX;
import static com.tc.license.util.LicenseConstants.EX_SESSIONS;
import static com.tc.license.util.LicenseConstants.FX;
import static com.tc.license.util.LicenseConstants.PRODUCT;
import static com.tc.license.util.LicenseConstants.PRODUCT_ENTERPRISE_SUITE;
import static com.tc.license.util.LicenseConstants.PRODUCT_SESSIONS;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import com.tc.license.License;
import com.tc.license.LicenseFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TerracottaLicenseFormat implements LicenseFormat {
  private static final String LICENSE_HEADER    = "--------------------- BEGIN TERRACOTTA LICENSE KEY ---------------------";
  private static final String LICENSE_SIGNATURE = "---------------------------- BEGIN SIGNATURE ---------------------------";
  private static final String LICENSE_FOOTER    = "---------------------- END TERRACOTTA LICENSE KEY ----------------------";
  
  private static final String DESCRIPTOR_V1     = "1";
  private static final String DESCRIPTOR_V2     = "2";

  public void store(OutputStream out, License license) {
    PrintWriter writer = new PrintWriter(out, true);
    writer.println("#" + LicenseConstants.DESCRIPTOR_VERSION + " = " + DESCRIPTOR_V2);
    writer.println(LICENSE_HEADER);
    writer.println(license.toString());
    writer.println(LICENSE_SIGNATURE);
    writer.println(license.getSignature());
    writer.println(LICENSE_FOOTER);
  }

  public License loadAndVerify(InputStream in, Signer signer) {
    List<String> lines;
    String descriptorVersion;
    try {
      lines = IOUtils.readLines(in);
      descriptorVersion = extractDescriptorVersion(lines);
    } catch (IOException e) {
      throw new LicenseException(e);
    }
    int headerIndex = lines.indexOf(LICENSE_HEADER);
    int footerIndex = lines.indexOf(LICENSE_FOOTER);
    int signatureIndex = lines.indexOf(LICENSE_SIGNATURE);

    boolean invalidFormat = headerIndex < 0 || footerIndex < 0 || signatureIndex < 0;
    if (invalidFormat) { throw new LicenseException("Product key has been compromised."); }

    List<String> properties = lines.subList(headerIndex + 1, signatureIndex);
    String signature = lines.get(signatureIndex + 1);
    verifySignature(signer, properties, signature, descriptorVersion);

    Map<String, String> fields = extractLicenseFields(properties);
    if (DESCRIPTOR_V1.equals(descriptorVersion)) {
      fields = upgradeFromV1(fields);
    }
    License license = LicenseFactory.createEnterpriseLicense(fields);
    license.setSignature(signature);
    return license;
  }

  private void verifySignature(Signer signer, List<String> properties, String signature, String descriptorVersion) {
    try {
      String rawLicenseAsString = StringUtils.join(properties.iterator(), "");
      if (DESCRIPTOR_V1.equals(descriptorVersion)) {
        rawLicenseAsString = rawLicenseAsString.replace(" = ", "");
      }
      boolean valid = signer.verify(rawLicenseAsString.getBytes(LicenseConstants.CANONICAL_ENCODING), signature);
      if (!valid) { throw new LicenseException("Product key has been compromised."); }
    } catch (UnsupportedEncodingException e) {
      throw new LicenseException(e);
    }
  }

  private String extractDescriptorVersion(List<String> lines) {
    for (String line : lines) {
      if (line.contains(LicenseConstants.DESCRIPTOR_VERSION)) {
        String[] tokens = line.split("\\s*=\\s*");
        if (tokens.length != 2) { throw new LicenseException("Error parsing descriptor version"); }
        return tokens[1];
      }
    }
    // we only start adding license format version starting with version 2
    // if we can't find it, default to version 1
    return DESCRIPTOR_V1;
  }

  private static Map<String, String> extractLicenseFields(List<String> lines) {
    Map<String, String> fieldsMap = new HashMap<String, String>();
    for (String line : lines) {
      if (line.trim().length() == 0) continue;
      String[] tokens = line.split("=", 2);
      fieldsMap.put(tokens[0].trim(), tokens[1].trim());
    }
    return fieldsMap;
  }

  private Map<String, String> upgradeFromV1(Map<String, String> fields) {
    String product = fields.get(PRODUCT);
    if (EX.equals(product)) {
      fields.put(PRODUCT, PRODUCT_ENTERPRISE_SUITE);
      fields.put(EDITION, EX);
    } else if (FX.equals(product)) {
      fields.put(PRODUCT, PRODUCT_ENTERPRISE_SUITE);
      fields.put(EDITION, FX);
    } else if (EX_SESSIONS.equals(product)) {
      fields.put(PRODUCT, PRODUCT_SESSIONS);
      fields.put(EDITION, EX);
    } else if (CUSTOM.equals(product)) {
      fields.put(PRODUCT, PRODUCT_ENTERPRISE_SUITE);
      fields.put(EDITION, FX);
    } else {
      throw new LicenseException("Failed to upgrade old product key. Product '" + product + "' unknown");
    }
    
    // since we're upgrading, let the new system figures out which are the proper capabilities
    // remove it here so it can be auto-generated
    fields.remove(LicenseConstants.CAPABILITIES);
    
    return fields;
  }

}
