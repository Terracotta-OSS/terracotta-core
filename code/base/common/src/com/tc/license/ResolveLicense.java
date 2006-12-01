/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.license;

import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.util.Resolve;

public final class ResolveLicense implements Resolve {

  public static final String     LICENSE            = "license.lic";
  private static final NoLicense NO_LICENSE_LICENSE = NoLicense.getInstance();

  private ResolveLicense() {
    // cannot instantiate
  }

  public static TerracottaLicense getLicense() throws InvalidLicenseException, ConfigurationSetupException {
    return NO_LICENSE_LICENSE;
  }
}
