/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.license.util;

import org.apache.commons.io.IOUtils;

import com.tc.license.License;
import com.tc.license.LicenseFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class TerracottaLicenseFormat implements LicenseFormat {
  private static final String LICENSE_HEADER    = "--------------------- BEGIN TERRACOTTA LICENSE KEY ---------------------";
  private static final String LICENSE_SIGNATURE = "---------------------------- BEGIN SIGNATURE ---------------------------";
  private static final String LICENSE_FOOTER    = "---------------------- END TERRACOTTA LICENSE KEY ----------------------";

  public void store(License license, OutputStream out) {
    PrintWriter writer = new PrintWriter(out, true);
    writer.println(LICENSE_HEADER);
    writer.println(license.toString());
    writer.println(LICENSE_SIGNATURE);
    writer.println(license.getSignature());
    writer.println(LICENSE_FOOTER);
  }

  public License load(InputStream in) throws LicenseException, IOException {
    List lines = IOUtils.readLines(in);
    int headerIndex = lines.indexOf(LICENSE_HEADER);
    int footerIndex = lines.indexOf(LICENSE_FOOTER);
    int signatureIndex = lines.indexOf(LICENSE_SIGNATURE);
    if (headerIndex < 0 || footerIndex < 0 || signatureIndex < 0) {
      //
      throw new LicenseException("License key structure has been compromised.");
    }
    List properties = lines.subList(headerIndex + 1, signatureIndex);
    String signatureString = (String) lines.get(signatureIndex + 1);
    Map<String, String> fields = extractLicenseFields(properties);
    License license = getLicenseFromFields(fields);
    license.setSignature(signatureString);
    return license;
  }

  private static Map<String, String> extractLicenseFields(List lines) {
    Map<String, String> fieldsMap = new HashMap<String, String>();
    for (Iterator it = lines.iterator(); it.hasNext();) {
      String line = (String) it.next();
      if (line.trim().length() == 0) continue;
      String[] tokens = line.split("=", 2);
      fieldsMap.put(tokens[0].trim(), tokens[1].trim());
    }
    return fieldsMap;
  }

  private static License getLicenseFromFields(Map<String, String> fields) throws LicenseException {
    String type = fields.get(LicenseConstants.LICENSE_TYPE);
    String number = fields.get(LicenseConstants.LICENSE_NUMBER);
    String licensee = fields.get(LicenseConstants.LICENSEE);
    String product = fields.get(LicenseConstants.PRODUCT);
    String maxClients = fields.get(LicenseConstants.MAX_CLIENTS);
    String expirationDate = fields.get(LicenseConstants.EXPIRATION_DATE);
    String capabilities = fields.get(LicenseConstants.CAPABILITIES);
    return LicenseFactory.createEnterpriseLicense(type, number, licensee, product, maxClients, expirationDate,
                                                  capabilities);
  }

}
