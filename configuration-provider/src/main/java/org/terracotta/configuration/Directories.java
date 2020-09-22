/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package org.terracotta.configuration;

import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Knows how to find certain directories. You should try <em>everything</em> you can to avoid using this class; using it
 * requires the user to set various system properties when running Terracotta, and we try to avoid that if at all
 * possible.
 */
public class Directories {

  /**
   * The property name is "tc.install-root".
   */
  public static final String TC_INSTALL_ROOT_PROPERTY_NAME               = "tc.install-root";

  /**
   * The property "tc.install-root.ignore-checks", which is used for testing to ignore checks for the installation root
   * directory.
   */
  public static final String TC_INSTALL_ROOT_IGNORE_CHECKS_PROPERTY_NAME = "tc.install-root.ignore-checks";

  /**
   * Relative location for server lib directory under Terracotta installation directory
   */
  public static final String SERVER_LIB_DIR                                  = "lib";

  /**
   * Relative location for server plugin api directory under Terracotta installation directory
   */
  public static final String SERVER_PLUGIN_API_DIR                           = "plugins/api";

  /**
   * Relative location for server plugin lib directory under Terracotta installation directory
   */
  public static final String SERVER_PLUGIN_LIB_DIR                           = "plugins/lib";

  /**
   * Relative location for default configuration file under Terracotta installation directory
   */
  public static final String DEFAULT_CONFIG_FILE_LOCATION                    = "conf/tc-config.xml";

  /**
   * Get installation root directory.
   * 
   * @return Installation root directory or {@code user.dir} if TC_INSTALL_ROOT_IGNORE_CHECKS_PROPERTY_NAME is set and
   *         TC_INSTALL_ROOT_PROPERTY_NAME is not.
   * @throws FileNotFoundException If {@link #TC_INSTALL_ROOT_IGNORE_CHECKS_PROPERTY_NAME} has not been set,
   *         this exception may be thrown if the installation root directory is not a directory
   */
  static File getInstallationRoot() throws FileNotFoundException {
    boolean ignoreCheck = Boolean.getBoolean(TC_INSTALL_ROOT_IGNORE_CHECKS_PROPERTY_NAME);
    if (ignoreCheck) {
      return new File(System.getProperty("user.dir"));
    } else {
      String path = System.getProperty(TC_INSTALL_ROOT_PROPERTY_NAME);
      if (path == null || path.trim().isEmpty()) {
        //if not set, use working dir
        path = System.getProperty("user.dir");
        LoggerFactory.getLogger(Directories.class).info("System property \"tc.install-root\" is not set, using working dir (" + path + ") as default location ");
      }

      File rootPath = new File(path).getAbsoluteFile();
      if (!rootPath.isDirectory()) {
        // formatting
        throw new FileNotFoundException("The specified Terracotta installation directory, '" + rootPath
                                        + "', located via the value of the system property '"
                                        + TC_INSTALL_ROOT_PROPERTY_NAME + "', does not actually exist.");
      }
      return rootPath;
    }
  }

  public static File getDefaultConfigFile() throws FileNotFoundException {
    return new File(getInstallationRoot(), DEFAULT_CONFIG_FILE_LOCATION);
  }

  public static File getServerLibFolder() throws FileNotFoundException {
    return new File(getInstallationRoot(), SERVER_LIB_DIR);
  }

  public static File getServerPluginsApiDir() throws FileNotFoundException {
    return new File(getInstallationRoot(), SERVER_PLUGIN_API_DIR);
  }

  public static File getServerPluginsLibDir() throws FileNotFoundException {
    return new File(getInstallationRoot(), SERVER_PLUGIN_LIB_DIR);
  }

}
