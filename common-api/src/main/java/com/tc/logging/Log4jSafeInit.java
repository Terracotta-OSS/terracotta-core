/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.logging;

import org.apache.log4j.Logger;
import org.apache.log4j.varia.NullAppender;

public class Log4jSafeInit {

  private static boolean initialized = false;

  public static void init() {
    // only run this code once (ever)
    synchronized (Log4jSafeInit.class) {
      if (initialized) return;
      initialized = true;
    }

    // This code is here because users can set various Log4J properties that will, for example, cause Log4J to try
    // to use arbitrary classes as appenders. If users try to use one of their own classes as an appender, we'll try
    // to load it in our classloader and fail in fairly unpleasant ways.
    //
    // This code also serves as a guard against libraries that might contain magic log4j config files
    //
    // Yes, saving and restoring a system property really sucks, but there isn't really a better way to do it. Users
    // can request that Log4J read config from an arbitrary URL otherwise, and there's no way to intercept that at
    // all. As a result, this seems like a better solution.
    //
    // See LKC-1974, DEV-2611 for more details.

    String oldDefaultInitOverrideValue = null;

    try {
      oldDefaultInitOverrideValue = System.setProperty("log4j.defaultInitOverride", "true");
      Logger.getRootLogger(); // inits log4j

      // Hack since Sigar insists on initlializing log4j itself based whether the root logger has any appenders
      // The setup provoked there might pick up log4j.properties from system classpath, etc
      Logger.getRootLogger().addAppender(new NullAppender());

    } finally {
      if (oldDefaultInitOverrideValue == null) {
        System.getProperties().remove("log4j.defaultInitOverride");
      } else {
        System.setProperty("log4j.defaultInitOverride", oldDefaultInitOverrideValue);
      }
    }
  }
}
