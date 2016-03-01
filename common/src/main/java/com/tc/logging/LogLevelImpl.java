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
package com.tc.logging;

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

  private final static int LOG4J_OFF_INT = Integer.MAX_VALUE;
  private final static int LOG4J_FATAL_INT = 50000;
  private final static int LOG4J_ERROR_INT = 40000;
  private final static int LOG4J_WARN_INT = 30000;
  private final static int LOG4J_INFO_INT = 20000;
  private final static int LOG4J_DEBUG_INT = 10000;
  private final static int LOG4J_ALL_INT = Integer.MIN_VALUE;

  private final int            level;

  private LogLevelImpl(int level) {
    this.level = level;
  }

  @Override
  public int getLevel() {
    return level;
  }

  @Override
  public boolean isInfo() {
    return level == LEVEL_INFO;
  }

  static LogLevel fromLog4JLevel(int level) {
    if(level >= LOG4J_OFF_INT) return OFF;
    if(level >= LOG4J_FATAL_INT) return FATAL;
    if(level >= LOG4J_ERROR_INT) return ERROR;
    if(level >= LOG4J_WARN_INT) return WARN;
    if(level >= LOG4J_INFO_INT) return INFO;
    if(level >= LOG4J_DEBUG_INT) return DEBUG;
    return DEBUG; // >= LOG4J_ALL_INT
  }

  @Override
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

}
