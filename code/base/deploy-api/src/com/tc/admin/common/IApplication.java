/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.common;


import java.util.prefs.Preferences;

public interface IApplication {
  String getName();
  String[] parseArgs(String[] args);
  void start();
  Preferences loadPrefs();
  void storePrefs();  
  ApplicationContext getApplicationContext();
}
