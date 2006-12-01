/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.license;

import java.util.Date;

/**
 * Represents a license to use Terracotta.
 */
public interface TerracottaLicense {

  public static final String   MODULE_DSO                          = "dso";
  public static final String   MODULE_SESSION_REPLICATION_WEBLOGIC = "wls-session";
  public static final String   MODULE_SESSION_REPLICATION_TOMCAT   = "tomcat-session";

  public static final String[] ALL_POSSIBLE_MODULES                = { MODULE_DSO,
      MODULE_SESSION_REPLICATION_TOMCAT, MODULE_SESSION_REPLICATION_WEBLOGIC };

  String licenseType();

  int maxL2Connections();

  long maxL2RuntimeMillis();

  Date l2ExpiresOn();

  int serialNumber();

  String licensee();

  boolean isModuleEnabled(String moduleName);

  String describe();
  
  boolean dsoHAEnabled();

}
