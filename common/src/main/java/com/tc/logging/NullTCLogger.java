/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.logging;


/**
 * @author steve
 */
public class NullTCLogger implements TCLogger {

  @Override
  public void debug(Object message) {
    //
  }

  @Override
  public void debug(Object message, Throwable t) {
    //
  }

  @Override
  public void error(Object message) {
    //
  }

  @Override
  public void error(Object message, Throwable t) {
    //
  }

  @Override
  public void fatal(Object message) {
    //
  }

  @Override
  public void fatal(Object message, Throwable t) {
    //
  }

  @Override
  public void info(Object message) {
    //
  }

  @Override
  public void info(Object message, Throwable t) {
    //
  }

  @Override
  public void warn(Object message) {
    //
  }

  @Override
  public void warn(Object message, Throwable t) {
    //
  }

  @Override
  public boolean isDebugEnabled() {
    return false;
  }

  @Override
  public boolean isInfoEnabled() {
    return false;
  }

  @Override
  public void setLevel(LogLevel level) {
    //
  }

  @Override
  public LogLevel getLevel() {
    throw new AssertionError();
  }

  @Override
  public String getName() {
    return "";
  }

}