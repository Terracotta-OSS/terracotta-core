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

import org.slf4j.Logger;
import org.slf4j.Marker;

public abstract class BaseMessageDecoratorTCLogger implements Logger {

  private final Logger logger;
  
  public BaseMessageDecoratorTCLogger(Logger logger) {
    this.logger = logger;
  }

  // Method base classes should implement
  protected abstract String decorate(Object message);
  
  @Override
  public String getName() {
    return logger.getName();
  }

  @Override
  public boolean isTraceEnabled() {
    return logger.isTraceEnabled();
  }

  @Override
  public void trace(String s) {
    logger.trace(decorate(s));
  }

  @Override
  public void trace(String s, Object o) {
    logger.trace(decorate(s), o);
  }

  @Override
  public void trace(String s, Object o, Object o1) {
    logger.trace(decorate(s), o, o1);
  }

  @Override
  public void trace(String s, Object... objects) {
    logger.trace(decorate(s), objects);
  }

  @Override
  public void trace(String s, Throwable throwable) {
    logger.trace(decorate(s), throwable);
  }

  @Override
  public boolean isTraceEnabled(Marker marker) {
    return logger.isTraceEnabled(marker);
  }

  @Override
  public void trace(Marker marker, String s) {
    logger.trace(marker, decorate(s));
  }

  @Override
  public void trace(Marker marker, String s, Object o) {
    logger.trace(marker, decorate(s), o);
  }

  @Override
  public void trace(Marker marker, String s, Object o, Object o1) {
    logger.trace(marker, decorate(s), o, o1);
  }

  @Override
  public void trace(Marker marker, String s, Object... objects) {
    logger.trace(marker, decorate(s), objects);
  }

  @Override
  public void trace(Marker marker, String s, Throwable throwable) {
    logger.trace(marker, decorate(s), throwable);
  }

  @Override
  public boolean isDebugEnabled() {
    return logger.isDebugEnabled();
  }

  @Override
  public void debug(String s) {
    logger.debug(decorate(s));
  }

  @Override
  public void debug(String s, Object o) {
    logger.debug(decorate(s), o);
  }

  @Override
  public void debug(String s, Object o, Object o1) {
    logger.debug(decorate(s), o, o1);
  }

  @Override
  public void debug(String s, Object... objects) {
    logger.debug(decorate(s), objects);
  }

  @Override
  public void debug(String s, Throwable throwable) {
    logger.debug(decorate(s), throwable);
  }

  @Override
  public boolean isDebugEnabled(Marker marker) {
    return logger.isDebugEnabled(marker);
  }

  @Override
  public void debug(Marker marker, String s) {
    logger.debug(marker, decorate(s));
  }

  @Override
  public void debug(Marker marker, String s, Object o) {
    logger.debug(marker, decorate(s), o);
  }

  @Override
  public void debug(Marker marker, String s, Object o, Object o1) {
    logger.debug(marker, decorate(s), o, o1);
  }

  @Override
  public void debug(Marker marker, String s, Object... objects) {
    logger.debug(marker, decorate(s), objects);
  }

  @Override
  public void debug(Marker marker, String s, Throwable throwable) {
    logger.debug(marker, decorate(s), throwable);
  }

  @Override
  public boolean isInfoEnabled() {
    return logger.isInfoEnabled();
  }

  @Override
  public void info(String s) {
    logger.info(decorate(s));
  }

  @Override
  public void info(String s, Object o) {
    logger.info(decorate(s), o);
  }

  @Override
  public void info(String s, Object o, Object o1) {
    logger.info(decorate(s), o, o1);
  }

  @Override
  public void info(String s, Object... objects) {
    logger.info(decorate(s), objects);
  }

  @Override
  public void info(String s, Throwable throwable) {
    logger.info(decorate(s), throwable);
  }

  @Override
  public boolean isInfoEnabled(Marker marker) {
    return logger.isInfoEnabled(marker);
  }

  @Override
  public void info(Marker marker, String s) {
    logger.info(marker, decorate(s));
  }

  @Override
  public void info(Marker marker, String s, Object o) {
    logger.info(marker, decorate(s), o);
  }

  @Override
  public void info(Marker marker, String s, Object o, Object o1) {
    logger.info(marker, decorate(s), o, o1);
  }

  @Override
  public void info(Marker marker, String s, Object... objects) {
    logger.info(marker, decorate(s), objects);
  }

  @Override
  public void info(Marker marker, String s, Throwable throwable) {
    logger.info(marker, decorate(s), throwable);
  }

  @Override
  public boolean isWarnEnabled() {
    return logger.isWarnEnabled();
  }

  @Override
  public void warn(String s) {
    logger.warn(decorate(s));
  }

  @Override
  public void warn(String s, Object o) {
    logger.warn(decorate(s), o);
  }

  @Override
  public void warn(String s, Object... objects) {
    logger.warn(decorate(s), objects);
  }

  @Override
  public void warn(String s, Object o, Object o1) {
    logger.warn(decorate(s), o, o1);
  }

  @Override
  public void warn(String s, Throwable throwable) {
    logger.warn(decorate(s), throwable);
  }

  @Override
  public boolean isWarnEnabled(Marker marker) {
    return logger.isWarnEnabled(marker);
  }

  @Override
  public void warn(Marker marker, String s) {
    logger.warn(marker, decorate(s));
  }

  @Override
  public void warn(Marker marker, String s, Object o) {
    logger.warn(marker, decorate(s), o);
  }

  @Override
  public void warn(Marker marker, String s, Object o, Object o1) {
    logger.warn(marker, decorate(s), o, o1);
  }

  @Override
  public void warn(Marker marker, String s, Object... objects) {
    logger.warn(marker, decorate(s), objects);
  }

  @Override
  public void warn(Marker marker, String s, Throwable throwable) {
    logger.warn(marker, decorate(s), throwable);
  }

  @Override
  public boolean isErrorEnabled() {
    return logger.isErrorEnabled();
  }

  @Override
  public void error(String s) {
    logger.error(decorate(s));
  }

  @Override
  public void error(String s, Object o) {
    logger.error(decorate(s), o);
  }

  @Override
  public void error(String s, Object o, Object o1) {
    logger.error(decorate(s), o, o1);
  }

  @Override
  public void error(String s, Object... objects) {
    logger.error(decorate(s), objects);
  }

  @Override
  public void error(String s, Throwable throwable) {
    logger.error(decorate(s), throwable);
  }

  @Override
  public boolean isErrorEnabled(Marker marker) {
    return logger.isErrorEnabled(marker);
  }

  @Override
  public void error(Marker marker, String s) {
    logger.error(marker, decorate(s));
  }

  @Override
  public void error(Marker marker, String s, Object o) {
    logger.error(marker, decorate(s), o);
  }

  @Override
  public void error(Marker marker, String s, Object o, Object o1) {
    logger.error(marker, decorate(s), o, o1);
  }

  @Override
  public void error(Marker marker, String s, Object... objects) {
    logger.error(marker, decorate(s), objects);
  }

  @Override
  public void error(Marker marker, String s, Throwable throwable) {
    logger.error(marker, decorate(s), throwable);
  }
}
