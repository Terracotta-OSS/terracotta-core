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
package com.tc.client.logging;

import com.tc.logging.LogLevel;
import com.tc.logging.LogLevels;
import com.tc.logging.TCLogger;
import org.slf4j.Logger;

/**
 *
 */
public class TCLoggerClientSLF4J implements TCLogger {
  
  private final Logger base;

  public TCLoggerClientSLF4J(Logger base) {
    this.base = base;
  }

  @Override
  public void debug(Object message) {
    base.debug(message.toString());
  }

  @Override
  public void debug(Object message, Throwable t) {
    base.debug(message.toString(), t);
  }

  @Override
  public void error(Object message) {
    base.error(message.toString());
  }

  @Override
  public void error(Object message, Throwable t) {
    base.error(message.toString(), t);
  }

  @Override
  public void fatal(Object message) {
    base.error(message.toString());
  }

  @Override
  public void fatal(Object message, Throwable t) {
    base.error(message.toString(), t);
  }

  @Override
  public void info(Object message) {
    base.info(message.toString());
  }

  @Override
  public void info(Object message, Throwable t) {
    base.info(message.toString(), t);
  }

  @Override
  public void warn(Object message) {
    base.warn(message.toString());
  }

  @Override
  public void warn(Object message, Throwable t) {
    base.warn(message.toString(), t);
  }

  @Override
  public boolean isDebugEnabled() {
    return base.isDebugEnabled();
  }

  @Override
  public boolean isInfoEnabled() {
    return base.isInfoEnabled();
  }

  @Override
  public void setLevel(LogLevel level) {

  }

  @Override
  public LogLevel getLevel() {
    return LogLevels.INFO;
  }

  @Override
  public String getName() {
    return base.getName();
  }
}
