package com.tc.license;

import com.tc.license.util.LicenseConstants;
import com.tc.license.util.LicenseDescriptor;
import com.tc.license.util.LicenseException;
import com.tc.license.util.LicenseField;

import java.util.Date;
import java.util.EnumSet;

public class LicenseFactory {

  /**
   * Creates Terracotta Enterprise license Capabilities will be automatically filled in according to the 'product'
   * 
   * @param type
   * @param number
   * @param licensee
   * @param product
   * @param maxClients
   * @param expiration
   * @return EnterpriseLicense
   * @throws LicenseException
   * @throws LicenseException if the license doesn't match with descriptor
   */
  public static License createEnterpriseLicense(String type, String number, String licensee, String product,
                                                String maxClients, String expirationDate) throws LicenseException {

    LicenseField productField = createField(LicenseConstants.PRODUCT, product);
    return createLicenseHelper(type, number, licensee, product, maxClients, expirationDate, LicenseDescriptor
        .getInstance().getLicensedCapabilities((String) productField.getValue()));
  }

  /**
   * <pre>
   * - Call this to recreate a license from raw info without capabilities lookup
   * - Useful to construct a License object with info reading from the product key
   * </pre>
   */
  public static License createEnterpriseLicense(String type, String number, String licensee, String product,
                                                String maxClients, String expirationDate, String licensedCapabilities)
      throws LicenseException {

    return createLicenseHelper(type, number, licensee, product, maxClients, expirationDate, Capability
        .toSet(licensedCapabilities));
  }

  public static License createOpenSourceLicense() {
    LicenseDescriptor descriptor = LicenseDescriptor.getInstance();
    Capabilities openSourceCapabilities = new Capabilities(descriptor.getLicensedCapabilities(LicenseConstants.ES),
                                                           descriptor.getOpenSourceCapabilities());
    return new OpenSourceLicense(openSourceCapabilities);
  }

  private static License createLicenseHelper(String type, String number, String licensee, String product,
                                             String maxClients, String expirationDate,
                                             EnumSet<Capability> licensedCapabilities) throws LicenseException {

    LicenseField typeField = createField(LicenseConstants.LICENSE_TYPE, type);
    LicenseField numberField = createField(LicenseConstants.LICENSE_NUMBER, number);
    LicenseField licenseeField = createField(LicenseConstants.LICENSEE, licensee);
    LicenseField maxClientField = createField(LicenseConstants.MAX_CLIENTS, maxClients);
    LicenseField productField = createField(LicenseConstants.PRODUCT, product);
    LicenseField expiredDateField = createField(LicenseConstants.EXPIRATION_DATE, expirationDate);

    Capabilities capabilities = new Capabilities(licensedCapabilities, LicenseDescriptor.getInstance()
        .getEnterpriseCapabilities());

    License license = new EnterpriseLicense((String) typeField.getValue(), (String) numberField.getValue(),
                                            (String) licenseeField.getValue(), (String) productField.getValue(),
                                            (Integer) maxClientField.getValue(), (Date) expiredDateField.getValue(),
                                            capabilities);

    return license;
  }

  private static LicenseField createField(String name, String value) throws LicenseException {
    LicenseField field = LicenseDescriptor.getInstance().createField(name);
    field.setRawValue(value);
    return field;
  }
}
