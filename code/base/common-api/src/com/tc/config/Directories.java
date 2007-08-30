/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config;

import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Knows how to find certain directories. You should try <em>everything</em> you can to avoid using this class; using
 * it requires the user to set various system properties when running Terracotta, and we try to avoid that if at all
 * possible.
 */
public class Directories {

  /**
   * This is <code>public</code> <strong>ONLY</strong> so that some entities can <strong>SET</strong> it. You should
   * <strong>NOT</strong> set it yourself; that breaks the point of encapsulation. Use the method 
   * {@link Environment#inTest()} instead.  The property name is "tc.install-root".  
   */
  public static final String TC_INSTALL_ROOT_PROPERTY_NAME               = "tc.install-root";
  
  /**
   * The property "tc.install-root.ignore-checks", which is used for testing to ignore checks for the installation 
   * root directory.
   */
  public static final String TC_INSTALL_ROOT_IGNORE_CHECKS_PROPERTY_NAME = "tc.install-root.ignore-checks";
  
  /**
   * The property "tc.license-location", which indicates the directory containing the license.
   */
  public static final String TC_LICENSE_LOCATION_PROPERTY_NAME           = "tc.license-location";

  /**
   * Get the location of the license directory based on the value of {@link #TC_LICENSE_LOCATION_PROPERTY_NAME}.
   * @return Directory containing license file, never null
   * @throws FileNotFoundException If the tc.license-location directory has not been set, the license 
   * directory does not exist, or exists but is not a directory
   */
  public static File getLicenseLocation() throws FileNotFoundException {
    String path = System.getProperty(TC_LICENSE_LOCATION_PROPERTY_NAME);
    if (StringUtils.isBlank(path)) { throw new FileNotFoundException(
                                                                     "The system property '"
                                                                         + TC_LICENSE_LOCATION_PROPERTY_NAME
                                                                         + "' has not been set. As such, the Terracotta license location directory cannot be located."); }
    File licenseDir = new File(path).getAbsoluteFile();
    if (!licenseDir.exists() || !licenseDir.isDirectory()) { throw new FileNotFoundException(
                                                                                             "The specified Terracotta installation directory, '"
                                                                                                 + licenseDir
                                                                                                 + "', located via the value of the system property '"
                                                                                                 + TC_LICENSE_LOCATION_PROPERTY_NAME
                                                                                                 + "', does not actually exist."); }
    return licenseDir;
  }

  /**
   * Get installation root directory.
   * @return Installation root directory, never null
   * @throws FileNotFoundException If {@link #TC_INSTALL_ROOT_PROPERTY_NAME} has not been set.  If 
   * {@link #TC_INSTALL_ROOT_IGNORE_CHECKS_PROPERTY_NAME} has not been set, this exception may be 
   * thrown if the installation root directory has not been set, is not a directory, or does not contain
   * a lib/tc.jar that is a file.  
   */
  public static File getInstallationRoot() throws FileNotFoundException {
    String path = System.getProperty(TC_INSTALL_ROOT_PROPERTY_NAME);

    if (StringUtils.isBlank(path)) {
      // formatting
      throw new FileNotFoundException(
                                      "The system property '"
                                          + TC_INSTALL_ROOT_PROPERTY_NAME
                                          + "' has not been set. As such, the Terracotta installation directory cannot be located.");
    }

    File theFile = new File(path).getAbsoluteFile();

    if (System.getProperty(TC_INSTALL_ROOT_IGNORE_CHECKS_PROPERTY_NAME) == null) {
      String absolutePath = theFile.getAbsolutePath();

      if (!theFile.exists()) {
        // formatting
        throw new FileNotFoundException("The specified Terracotta installation directory, '" + absolutePath
                                        + "', located via the value of the system property '"
                                        + TC_INSTALL_ROOT_PROPERTY_NAME + "', does not actually exist.");
      }

      if (!theFile.isDirectory()) {
        // formatting
        throw new FileNotFoundException("The specified Terracotta installation directory, '" + absolutePath
                                        + "', located via the value of the system property '"
                                        + TC_INSTALL_ROOT_PROPERTY_NAME + "', does not actually exist.");
      }

      File searchFile = new File(new File(theFile, "lib"), "tc.jar");

      if (!searchFile.exists() || !searchFile.isFile()) {
        // This is just so we don't have to have tc.jar around in development configurations.
        if (new File(theFile, ".force-is-terracotta-install-dir").exists()) return theFile;
        else {
          // formatting
          throw new FileNotFoundException("The specified Terracotta installation directory, '" + absolutePath
                                          + "', located via the value of the system property '"
                                          + TC_INSTALL_ROOT_PROPERTY_NAME + "', does not seem to actually "
                                          + "be the root of the Terracotta installation. (The required "
                                          + "Terracotta JAR file, '" + searchFile.getAbsolutePath()
                                          + "', does not exist or is not a file.)");
        }
      }
    }

    return theFile;
  }

}
