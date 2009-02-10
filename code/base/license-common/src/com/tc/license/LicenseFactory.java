package com.tc.license;

import com.tc.license.util.LicenseConstants;
import com.tc.license.util.LicenseDescriptor;
import com.tc.license.util.LicenseException;
import com.tc.license.util.LicenseField;

import java.util.Date;
import java.util.EnumSet;

public class LicenseFactory {
  private static final LicenseDescriptor descriptor = new LicenseDescriptor();

  public static EnterpriseLicense createEnterpriseLicense(String licenseType, String licenseNumber, String licensee,
                                                          String product, String maxClients, String expirationDate)
      throws LicenseException {

    LicenseField productField = createField(LicenseConstants.PRODUCT, product);
    EnumSet<Capability> licensedCapabilities = descriptor.getLicensedCapabilities((String) productField.getValue());

    return createLicense(licenseType, licenseNumber, licensee, product, maxClients, expirationDate,
                         licensedCapabilities);
  }

  public static EnterpriseLicense createEnterpriseLicense(String licenseType, String licenseNumber, String licensee,
                                                          String product, String maxClients, String expirationDate,
                                                          String licensedCapabilities) throws LicenseException {

    return createLicense(licenseType, licenseNumber, licensee, product, maxClients, expirationDate, Capability
        .toSet(licensedCapabilities));
  }

  public static OpenSourceLicense createOpenSourceLicense() {
    Capabilities openSourceCapabilities = new Capabilities(descriptor.getLicensedCapabilities(LicenseConstants.ES),
                                                           descriptor.getOpenSourceCapabilities());
    return new OpenSourceLicense(openSourceCapabilities);
  }

  private static EnterpriseLicense createLicense(String type, String number, String licensee, String product,
                                                 String maxClients, String expirationDate,
                                                 EnumSet<Capability> licensedCapabilities) throws LicenseException {

    LicenseField typeField = createField(LicenseConstants.LICENSE_TYPE, type);
    LicenseField numberField = createField(LicenseConstants.LICENSE_NUMBER, number);
    LicenseField licenseeField = createField(LicenseConstants.LICENSEE, licensee);
    LicenseField maxClientField = createField(LicenseConstants.MAX_CLIENTS, maxClients);
    LicenseField productField = createField(LicenseConstants.PRODUCT, product);
    LicenseField expiredDateField = createField(LicenseConstants.EXPIRATION_DATE, expirationDate);

    Capabilities capabilities = new Capabilities(licensedCapabilities, new LicenseDescriptor()
        .getEnterpriseCapabilities());

    EnterpriseLicense license = new EnterpriseLicense((String) typeField.getValue(), (String) numberField.getValue(),
                                                      (String) licenseeField.getValue(), (String) productField
                                                          .getValue(), (Integer) maxClientField.getValue(),
                                                      (Date) expiredDateField.getValue(), capabilities);

    return license;
  }

  private static LicenseField createField(String name, String value) throws LicenseException {
    LicenseField field = descriptor.createField(name);
    field.setRawValue(value);
    return field;
  }
}
