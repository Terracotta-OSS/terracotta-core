/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.license;

import org.terracotta.license.EnterpriseLicenseResolverFactory;

import com.tc.properties.TCPropertiesImpl;

public class TerracottaLicenseResolver extends EnterpriseLicenseResolverFactory {
  public TerracottaLicenseResolver() {
    //
  }

  @Override
  protected String getProperty(String key) {
    if (key.startsWith(TCPropertiesImpl.SYSTEM_PROP_PREFIX)) {
      key = key.substring(TCPropertiesImpl.SYSTEM_PROP_PREFIX.length());
      return TCPropertiesImpl.getProperties().getProperty(key, true);
    } else {
      return System.getProperty(key);
    }
  }
}
