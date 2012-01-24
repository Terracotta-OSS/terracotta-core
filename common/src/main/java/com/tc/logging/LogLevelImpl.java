/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.logging;

import org.apache.log4j.Level;
import org.apache.log4j.Priority;

import com.tc.util.Assert;

/**
 * Defines constants for various logging levels
 * 
 * @author teck
 */
public class LogLevelImpl implements LogLevel {
  static final int             LEVEL_DEBUG = 5;
  static final int             LEVEL_INFO  = 4;
  static final int             LEVEL_WARN  = 3;
  static final int             LEVEL_ERROR = 2;
  static final int             LEVEL_FATAL = 1;
  static final int             LEVEL_OFF   = 0;

  public static final LogLevel DEBUG       = new LogLevelImpl(LEVEL_DEBUG);
  public static final LogLevel INFO        = new LogLevelImpl(LEVEL_INFO);
  public static final LogLevel WARN        = new LogLevelImpl(LEVEL_WARN);
  public static final LogLevel ERROR       = new LogLevelImpl(LEVEL_ERROR);
  public static final LogLevel FATAL       = new LogLevelImpl(LEVEL_FATAL);
  public static final LogLevel OFF         = new LogLevelImpl(LEVEL_OFF);

  public static final String   DEBUG_NAME  = "DEBUG";
  public static final String   INFO_NAME   = "INFO";
  public static final String   WARN_NAME   = "WARN";
  public static final String   ERROR_NAME  = "ERROR";
  public static final String   FATAL_NAME  = "FATAL";
  public static final String   OFF_NAME    = "OFF";

  private final int            level;

  private LogLevelImpl(int level) {
    this.level = level;
  }

  public int getLevel() {
    return level;
  }

  public boolean isInfo() {
    return level == LEVEL_INFO;
  }

  static Level toLog4JLevel(LogLevel level) {
    if (level == null) return null;

    switch (level.getLevel()) {
      case LEVEL_DEBUG:
        return Level.DEBUG;
      case LEVEL_INFO:
        return Level.INFO;
      case LEVEL_WARN:
        return Level.WARN;
      case LEVEL_ERROR:
        return Level.ERROR;
      case LEVEL_FATAL:
        return Level.FATAL;
      case LEVEL_OFF:
        return Level.OFF;
      default:
        throw Assert.failure("Logic Error: Invalid Level: " + level);
    }
  }

  static LogLevel fromLog4JLevel(Level level) {
    if (level == null) return null;
    switch (level.toInt()) {
      case Priority.DEBUG_INT:
        return DEBUG;
      case Priority.INFO_INT:
        return INFO;
      case Priority.WARN_INT:
        return WARN;
      case Priority.ERROR_INT:
        return ERROR;
      case Priority.FATAL_INT:
        return FATAL;
      case Priority.OFF_INT:
        return OFF;
      default:
        throw Assert.failure("Unsupported Level" + level);
    }
  }

  public String toString() {
    switch (getLevel()) {
      case LEVEL_DEBUG:
        return DEBUG_NAME;
      case LEVEL_INFO:
        return INFO_NAME;
      case LEVEL_WARN:
        return WARN_NAME;
      case LEVEL_ERROR:
        return ERROR_NAME;
      case LEVEL_FATAL:
        return FATAL_NAME;
      case LEVEL_OFF:
        return OFF_NAME;
      default:
        return "Unknown";
    }
  }

  public static LogLevel valueOf(String v) {
    if (DEBUG_NAME.equals(v)) {
      return DEBUG;
    } else if (INFO_NAME.equals(v)) {
      return INFO;
    } else if (WARN_NAME.equals(v)) {
      return WARN;
    } else if (ERROR_NAME.equals(v)) {
      return ERROR;
    } else if (FATAL_NAME.equals(v)) {
      return FATAL;
    } else if (OFF_NAME.equals(v)) {
      return OFF;
    } else {
      return null;
    }
  }

}
