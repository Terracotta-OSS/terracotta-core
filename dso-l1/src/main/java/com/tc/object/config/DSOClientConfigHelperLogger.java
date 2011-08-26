/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.config;

import com.tc.logging.TCLogger;

/**
 * Class containing log methods for {@link DSOClientConfigHelper}.
 */
public class DSOClientConfigHelperLogger {
  private final TCLogger LOGGER;

  DSOClientConfigHelperLogger(TCLogger logger) {
    this.LOGGER = logger;
  }

  void logIsLockMethodNoMatch(String className, String methodName) {
    if (LOGGER.isDebugEnabled()) LOGGER.debug("isLockMethod() " + className + "." + methodName + ": NO MATCH");
  }

  void logIsLockMethodMatch(Lock[] locks, String className, String methodName, int i) {
    if (LOGGER.isDebugEnabled()) LOGGER.debug("isLockMethod() " + className + "." + methodName + ": FOUND A MATCH: "
                                              + locks[i]);
  }

  void logIsLockMethodAutolock() {
    LOGGER.debug("isLockMethod(): is autolock and is method is synchronized, returning true.");
  }

  void logIsLockMethodBegin(int modifiers, String className, String methodName, String description) {
    if (LOGGER.isDebugEnabled()) LOGGER.debug("isLockMethod(" + modifiers + ", " + className + ", " + methodName + ", "
                                              + description + ")");
  }

}