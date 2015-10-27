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
 * Common TC logger interface (mostly a copy of the log4j logger interface)
 * 
 * @author teck
 */
public interface TCLogger {
  void debug(Object message);

  void debug(Object message, Throwable t);

  void error(Object message);

  void error(Object message, Throwable t);

  void fatal(Object message);

  void fatal(Object message, Throwable t);

  void info(Object message);

  void info(Object message, Throwable t);

  void warn(Object message);

  void warn(Object message, Throwable t);

  boolean isDebugEnabled();

  boolean isInfoEnabled();

  void setLevel(LogLevel level);

  LogLevel getLevel();

  String getName();
}
