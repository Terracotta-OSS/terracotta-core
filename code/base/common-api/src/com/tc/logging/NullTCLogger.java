/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.logging;


/**
 * @author steve
 */
public class NullTCLogger implements TCLogger {

  public void debug(Object message) {
    //
  }

  public void debug(Object message, Throwable t) {
    //
  }

  public void error(Object message) {
    //
  }

  public void error(Object message, Throwable t) {
    //
  }

  public void fatal(Object message) {
    //
  }

  public void fatal(Object message, Throwable t) {
    //
  }

  public void info(Object message) {
    //
  }

  public void info(Object message, Throwable t) {
    //
  }

  public void warn(Object message) {
    //
  }

  public void warn(Object message, Throwable t) {
    //
  }

  public boolean isDebugEnabled() {
    return false;
  }

  public boolean isInfoEnabled() {
    return false;
  }

  public void setLevel(LogLevel level) {
    //
  }

  public LogLevel getLevel() {
    throw new AssertionError();
  }

  public String getName() {
    return "";
  }

}