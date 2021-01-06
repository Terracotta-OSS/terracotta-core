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
package com.tc.server;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Paths;

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
   */
  public static final String TC_PLUGINS_ROOT_PROPERTY_NAME               = "tc.plugins-dir";
  /**
   */
  public static final String TC_PLUGINS_API_PROPERTY_NAME               = "tc.plugins-api";
   /**
   */
  public static final String TC_PLUGINS_LIB_PROPERTY_NAME               = "tc.plugins-lib";
   /**
   */
  public static final String TC_SERVER_DIR_PROPERTY_NAME               = "tc.server-dir";
  /**
   */
  public static final String TC_SERVER_LIB_PROPERTY_NAME               = "tc.server-lib";
  /**
   * The property "tc.install-root.ignore-checks", which is used for testing to ignore checks for the installation root
   * directory.
   */
  public static final String TC_INSTALL_ROOT_IGNORE_CHECKS_PROPERTY_NAME = "tc.install-root.ignore-checks";
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
        System.err.println("System property \"tc.install-root\" is not set, using working dir (" + path + ") as default location ");
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
    String installRoot = System.getProperty(TC_INSTALL_ROOT_PROPERTY_NAME, System.getProperty("user.dir"));
    String serverRoot = System.getProperty(TC_SERVER_DIR_PROPERTY_NAME, "server");
    String serverLib = System.getProperty(TC_PLUGINS_LIB_PROPERTY_NAME, "lib");
    File f = Paths.get(installRoot, serverRoot, serverLib).toFile();
    if (!f.isDirectory()) {
      throw new FileNotFoundException("server library folder at " + f.getAbsolutePath() + " is not valid");
    }
    return f;
  }

  public static File getServerPluginsApiDir() throws FileNotFoundException {
    String installRoot = System.getProperty(TC_INSTALL_ROOT_PROPERTY_NAME, System.getProperty("user.dir"));
    String pluginsRoot = System.getProperty(TC_PLUGINS_ROOT_PROPERTY_NAME, "plugins");
    String pluginsApi = System.getProperty(TC_PLUGINS_API_PROPERTY_NAME, "api");
    File f = Paths.get(installRoot, pluginsRoot, pluginsApi).toFile();
    if (!f.isDirectory()) {
      throw new FileNotFoundException("server plugins api folder at " + f.getAbsolutePath() + " is not valid");
    }
    return f;
  }

  public static File getServerPluginsLibDir() throws FileNotFoundException {
    String installRoot = System.getProperty(TC_INSTALL_ROOT_PROPERTY_NAME, System.getProperty("user.dir"));
    String pluginsRoot = System.getProperty(TC_PLUGINS_ROOT_PROPERTY_NAME, "plugins");
    String pluginsLib = System.getProperty(TC_PLUGINS_LIB_PROPERTY_NAME, "lib");
    File f = Paths.get(installRoot, pluginsRoot, pluginsLib).toFile();
    if (!f.isDirectory()) {
      throw new FileNotFoundException("server plugins implementations folder at " + f.getAbsolutePath() + " is not valid");
    }
    return f;
  }

}
