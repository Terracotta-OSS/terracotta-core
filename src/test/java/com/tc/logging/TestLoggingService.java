/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.tc.logging;

/**
 *
 * @author cdennis
 */
public class TestLoggingService implements TCLoggingService {

  @Override
  public TCLogger getLogger(String name) {
    return new TCLogger() {

      private void log(String level, String message) {
        System.out.println(level + " [" + getName() + "] : " + message);
      }
      
      @Override
      public void debug(Object message) {
        log("DEBUG", message.toString());
      }

      @Override
      public void debug(Object message, Throwable t) {
        debug(message + " : " + t);
      }

      @Override
      public void error(Object message) {
        log("ERROR", message.toString());
      }

      @Override
      public void error(Object message, Throwable t) {
        error(message + " : " + t);
      }

      @Override
      public void fatal(Object message) {
        log("FATAL", message.toString());
      }

      @Override
      public void fatal(Object message, Throwable t) {
        fatal(message + " : " + t);
      }

      @Override
      public void info(Object message) {
        log("INFO", message.toString());
      }

      @Override
      public void info(Object message, Throwable t) {
        info(message + " : " + t);
      }

      @Override
      public void warn(Object message) {
        log("WARN", message.toString());
      }

      @Override
      public void warn(Object message, Throwable t) {
        warn(message + " : " + t);
      }

      @Override
      public boolean isDebugEnabled() {
        return true;
      }

      @Override
      public boolean isInfoEnabled() {
        return true;
      }

      @Override
      public void setLevel(LogLevel level) {
        //no-op
      }

      @Override
      public LogLevel getLevel() {
        return null;
      }

      @Override
      public String getName() {
        return name;
      }
    };
  }

  @Override
  public TCLogger getLogger(Class<?> c) {
    return getLogger(c.getName());
  }
}
