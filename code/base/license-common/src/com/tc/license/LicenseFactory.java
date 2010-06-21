package com.tc.license;

import com.tc.license.util.LicenseConstants;
import com.tc.license.util.LicenseDescriptor;
import com.tc.license.util.LicenseField;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class LicenseFactory {
  private static final LicenseDescriptor descriptor = new LicenseDescriptor();

  public static License createEnterpriseLicense(Map<String, String> rawValueFields) {
    String product = rawValueFields.get(LicenseConstants.PRODUCT);
    String edition = rawValueFields.get(LicenseConstants.EDITION);
    Set<Capability> licensedCapabilities;
    if (LicenseConstants.PRODUCT_CUSTOM.equals(product)) {
      String listOfCapabilities = rawValueFields.get(LicenseConstants.CAPABILITIES);
      licensedCapabilities = toCapabilitiesSet(listOfCapabilities);
    } else {
      licensedCapabilities = descriptor.getLicensedCapabilities(product, edition);
    }
    Capabilities capabilities = new Capabilities(licensedCapabilities, descriptor.getEnterpriseCapabilities());
    return new EnterpriseLicense(covertToLicenseFields(rawValueFields), capabilities);
  }

  public static OpenSourceLicense createOpenSourceLicense() {
    Capabilities openSourceCapabilities = new Capabilities(descriptor.getOpenSourceCapabilities(),
                                                           descriptor.getOpenSourceCapabilities());
    return new OpenSourceLicense(openSourceCapabilities);
  }

  private static Map<String, Object> covertToLicenseFields(Map<String, String> rawValueFields) {
    Map<String, Object> convertedLicenseFields = new HashMap<String, Object>();

    for (Map.Entry<String, String> entry : rawValueFields.entrySet()) {
      // capabilities is auto generated, no conversion needed
      if (LicenseConstants.CAPABILITIES.equals(entry.getKey())) continue;
      
      LicenseField licenseField = descriptor.createField(entry.getKey());
      licenseField.setRawValue(entry.getValue());
      convertedLicenseFields.put(entry.getKey(), licenseField.getValue());
    }

    return convertedLicenseFields;
  }

  private static Set<Capability> toCapabilitiesSet(String listOfCapabilities) {
    Set<Capability> retVal = new HashSet<Capability>();
    String[] tokens = listOfCapabilities.split("\\s*,\\s*");
    for (String token : tokens) {
      retVal.add(new Capability(token));
    }
    return retVal;
  }
}
