/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.logging;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JDKLogging {

  // maintain refs to loggers to avoid potential problems with OpenJDK use of weak refs
  private static final Map<String, Logger> STRONG_REFS = new ConcurrentHashMap<String, Logger>();

  public static void setLevel(String loggerName, Level level) {
    Logger logger = STRONG_REFS.get(loggerName);

    if (logger == null) {
      logger = Logger.getLogger(loggerName);
      STRONG_REFS.put(loggerName, logger);
    }

    logger.setLevel(level);
  }
}
