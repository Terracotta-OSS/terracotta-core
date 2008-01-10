/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.weblogic;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;

import java.lang.reflect.Method;

public class WeblogicHelper {

  private static final TCLogger logger = TCLogging.getLogger(WeblogicHelper.class);

  public static boolean isWeblogicPresent() {
    // The first implementation I tried here was to actually load the "weblogic.Server" class. That is a reasonable
    // test, but loading it before the class adapter for it was registered was a bit self defeating :-)

    return ClassLoader.getSystemClassLoader().getResource("weblogic/Server.class") != null;
  }

  public static boolean isWL8() {
    try {
      String version = getVersion();
      return version.startsWith("8");
    } catch (Exception e) {
      return false;
    }
  }

  public static boolean isWL9() {
    try {
      String version = getVersion();
      return version.startsWith("9");
    } catch (Exception e) {
      return false;
    }
  }

  private static String getVersion() throws Exception {
    Class version = ClassLoader.getSystemClassLoader().loadClass("weblogic.version");
    Method getReleaseBuildVersion = version.getDeclaredMethod("getReleaseBuildVersion", new Class[] {});
    return (String) getReleaseBuildVersion.invoke(version, new Object[] {});
  }

  public static boolean isSupportedVersion() {
    try {
      String ver = getVersion();
      logger.info("Detected weblogic version: " + ver);
      return ver != null && (ver.startsWith("8.1") || ver.startsWith("9"));
    } catch (Exception e) {
      logger.error("Error trying to determine weblogic version", e);
      return false;
    }

    // unreachable
  }

}
