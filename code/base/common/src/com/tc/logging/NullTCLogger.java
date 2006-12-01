/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.logging;

import com.tc.exception.ImplementMe;

/**
 * @author steve
 */
public class NullTCLogger implements TCLogger {

  /*
   * (non-Javadoc)
   * 
   * @see com.tc.logging.TCLogger#debug(java.lang.Object)
   */
  public void debug(Object message) {
    // TODO Auto-generated method stub

  }

  /*
   * (non-Javadoc)
   * 
   * @see com.tc.logging.TCLogger#debug(java.lang.Object, java.lang.Throwable)
   */
  public void debug(Object message, Throwable t) {
    // TODO Auto-generated method stub

  }

  /*
   * (non-Javadoc)
   * 
   * @see com.tc.logging.TCLogger#error(java.lang.Object)
   */
  public void error(Object message) {
    // TODO Auto-generated method stub

  }

  /*
   * (non-Javadoc)
   * 
   * @see com.tc.logging.TCLogger#error(java.lang.Object, java.lang.Throwable)
   */
  public void error(Object message, Throwable t) {
    // TODO Auto-generated method stub

  }

  /*
   * (non-Javadoc)
   * 
   * @see com.tc.logging.TCLogger#fatal(java.lang.Object)
   */
  public void fatal(Object message) {
    // TODO Auto-generated method stub

  }

  /*
   * (non-Javadoc)
   * 
   * @see com.tc.logging.TCLogger#fatal(java.lang.Object, java.lang.Throwable)
   */
  public void fatal(Object message, Throwable t) {
    // TODO Auto-generated method stub

  }

  /*
   * (non-Javadoc)
   * 
   * @see com.tc.logging.TCLogger#info(java.lang.Object)
   */
  public void info(Object message) {
    // TODO Auto-generated method stub

  }

  /*
   * (non-Javadoc)
   * 
   * @see com.tc.logging.TCLogger#info(java.lang.Object, java.lang.Throwable)
   */
  public void info(Object message, Throwable t) {
    // TODO Auto-generated method stub

  }

  /*
   * (non-Javadoc)
   * 
   * @see com.tc.logging.TCLogger#warn(java.lang.Object)
   */
  public void warn(Object message) {
    // TODO Auto-generated method stub

  }

  /*
   * (non-Javadoc)
   * 
   * @see com.tc.logging.TCLogger#warn(java.lang.Object, java.lang.Throwable)
   */
  public void warn(Object message, Throwable t) {
    // TODO Auto-generated method stub

  }

  /*
   * (non-Javadoc)
   * 
   * @see com.tc.logging.TCLogger#isDebugEnabled()
   */
  public boolean isDebugEnabled() {
    // TODO Auto-generated method stub
    return false;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.tc.logging.TCLogger#isInfoEnabled()
   */
  public boolean isInfoEnabled() {
    // TODO Auto-generated method stub
    return false;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.tc.logging.TCLogger#log(com.tc.logging.LogLevel, java.lang.Object)
   */
  public void log(LogLevel level, Object message) {
    // TODO Auto-generated method stub

  }

  /*
   * (non-Javadoc)
   * 
   * @see com.tc.logging.TCLogger#log(com.tc.logging.LogLevel, java.lang.Object, java.lang.Throwable)
   */
  public void log(LogLevel level, Object message, Throwable t) {
    // TODO Auto-generated method stub

  }

  public void setLevel(LogLevel level) {
    // TODO Auto-generated method stub
  }

  public LogLevel getLevel() {
    if (true)throw new ImplementMe();
    return null;
  }

}