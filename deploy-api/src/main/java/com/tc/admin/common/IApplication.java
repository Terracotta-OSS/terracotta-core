/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
