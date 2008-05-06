/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.server.appserver.deployment;

import com.tc.test.TempDirectoryHelper;

import java.io.File;
import java.io.IOException;

public class TempDirectoryUtil {

  private static TempDirectoryHelper tempDirectoryHelper;

  public static File getTempDirectory(Class type) throws IOException {
    return getTempDirectoryHelper(type).getDirectory();
  }

  protected static synchronized TempDirectoryHelper getTempDirectoryHelper(Class type) {
    if (tempDirectoryHelper == null) {
      PropertiesHackForRunningInEclipse.initializePropertiesWhenRunningInEclipse();
      tempDirectoryHelper = new TempDirectoryHelper(type, true);
    }

    return tempDirectoryHelper;
  }

}
