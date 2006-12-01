/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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
public class LogLevel {
  static final int             LEVEL_DEBUG = 4;
  static final int             LEVEL_INFO  = 3;
  static final int             LEVEL_WARN  = 2;
  static final int             LEVEL_ERROR = 1;
  static final int             LEVEL_FATAL = 0;

  public static final LogLevel DEBUG       = new LogLevel(LEVEL_DEBUG);
  public static final LogLevel INFO        = new LogLevel(LEVEL_INFO);
  public static final LogLevel WARN        = new LogLevel(LEVEL_WARN);
  public static final LogLevel ERROR       = new LogLevel(LEVEL_ERROR);
  public static final LogLevel FATAL       = new LogLevel(LEVEL_FATAL);

  public static final String DEBUG_NAME = "DEBUG";
  public static final String INFO_NAME  = "INFO" ;
  public static final String WARN_NAME  = "WARN" ;
  public static final String ERROR_NAME = "ERROR";
  public static final String FATAL_NAME = "FATAL";
  
  private final int            level;

  private LogLevel(int level) {
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
    
    switch( level.getLevel() ) {
      case LEVEL_DEBUG: return Level.DEBUG;
      case LEVEL_INFO : return Level.INFO;
      case LEVEL_WARN : return Level.WARN;
      case LEVEL_ERROR: return Level.ERROR;
      case LEVEL_FATAL: return Level.FATAL;
      default         : throw Assert.failure( "Logic Error: Invalid Level: " + level);
    }
  }
  
  static LogLevel fromLog4JLevel(Level level) {
    if (level == null) return null;
    switch( level.toInt()) {
      case Priority.DEBUG_INT: return LogLevel.DEBUG;
      case Priority.INFO_INT : return LogLevel.INFO;
      case Priority.WARN_INT : return LogLevel.WARN;
      case Priority.ERROR_INT: return LogLevel.ERROR;
      case Priority.FATAL_INT: return LogLevel.FATAL;
      default                : throw Assert.failure("Unsupported Level" + level );
    }
  }

  public String toString() {
      switch( getLevel() ) {
        case LEVEL_DEBUG: return DEBUG_NAME;
        case LEVEL_INFO : return INFO_NAME;
        case LEVEL_WARN : return WARN_NAME;
        case LEVEL_ERROR: return ERROR_NAME;
        case LEVEL_FATAL: return FATAL_NAME;
        default         : return "Unknown";
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
    } else {
      return null;
    }
  }
  

}

