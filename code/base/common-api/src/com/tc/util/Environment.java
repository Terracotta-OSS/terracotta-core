/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

import com.tc.config.Directories;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Helper class for determining the runtime environment of Terracotta.
 */
public abstract class Environment {

  /**
   * Check if the environment running is in test mode based on absence/presence of 
   * the installation root directory.
   * @return <code>true</code> if in test mode.
   */
  public final static boolean inTest() {
    try {
      final File installRoot = Directories.getInstallationRoot();
      return (installRoot.getAbsolutePath().length() == 0);
    } catch (FileNotFoundException fnfe) {
      // ignore, tc.install-dir is not set so we must be in a test environment
      return true;
    }
  }
  
}
