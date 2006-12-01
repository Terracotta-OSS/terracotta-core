/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.test.server.appserver;

/**
 * Appserver package framework constants.
 */
interface AppServerConstants {

  // subdirectory of appserver working directory containing data files shared across all instances
  static final String DATA_DIR     = "data";
  // subdirectory of appserver working directory containing specific appserver instances
//  static final String SANDBOX      = "sandbox";

  // Default system properties available to appserver instances (used by servlets)
  static final String APP_INSTANCE = "app_instance"; // appserver instance name
  static final String APP_PORT     = "app_port";    // appserver instance port
}
