/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.license;

import java.util.Date;

public class NoLicense implements TerracottaLicense {

  private static final NoLicense INSTANCE      = new NoLicense();

  public static final String     TYPE          = "none";
  private static final int       SERIAL_NUMBER = 10000000;
  private static final String    LICENSEE      = "Terracotta Null Licensee";

  public static NoLicense getInstance() {
    return INSTANCE;
  }

  private NoLicense() {
    // nothing here
  }

  public String licenseType() {
    return TYPE;
  }

  public int maxL2Connections() {
    return -1;
  }

  public long maxL2RuntimeMillis() {
    return Long.MAX_VALUE;
  }

  public Date l2ExpiresOn() {
    return null;
  }

  public int serialNumber() {
    return SERIAL_NUMBER;
  }

  public String licensee() {
    return LICENSEE;
  }

  public boolean isModuleEnabled(String moduleName) {
    return true;
  }

  public String describe() {
    return "Open Source - No License Needed";
  }
  
  public boolean dsoHAEnabled() {
    return true;
  }

}
