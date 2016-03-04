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
package com.tc.client;

import com.tc.logging.LogLevel;
import com.tc.logging.LogLevels;
import com.tc.logging.TCLogger;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class TCLoggerClient implements TCLogger {
  
  private final Logger base;

  public TCLoggerClient(Logger base) {
    this.base = base;
  }

  @Override
  public void debug(Object message) {
    base.finer(message.toString());
  }

  @Override
  public void debug(Object message, Throwable t) {
    base.finer(message.toString());
    t.printStackTrace(new PrintWriter(new Writer() {
      @Override
      public void write(char[] cbuf, int off, int len) throws IOException {
        base.finer(new String(cbuf,off,len));
      }

      @Override
      public void flush() throws IOException {

      }

      @Override
      public void close() throws IOException {

      }
    }));
  }

  @Override
  public void error(Object message) {
    base.warning(message.toString());
  }

  @Override
  public void error(Object message, Throwable t) {
    base.warning(message.toString());
    t.printStackTrace(new PrintWriter(new Writer() {
      @Override
      public void write(char[] cbuf, int off, int len) throws IOException {
        base.warning(new String(cbuf,off,len));
      }

      @Override
      public void flush() throws IOException {

      }

      @Override
      public void close() throws IOException {

      }
    }));
  }

  @Override
  public void fatal(Object message) {
    base.severe(message.toString());
  }

  @Override
  public void fatal(Object message, Throwable t) {
    base.severe(message.toString());
    t.printStackTrace(new PrintWriter(new Writer() {
      @Override
      public void write(char[] cbuf, int off, int len) throws IOException {
        base.severe(new String(cbuf,off,len));
      }

      @Override
      public void flush() throws IOException {

      }

      @Override
      public void close() throws IOException {

      }
    }));
  }

  @Override
  public void info(Object message) {
    base.info(message.toString());
  }

  @Override
  public void info(Object message, Throwable t) {
    base.info(message.toString());
    t.printStackTrace(new PrintWriter(new Writer() {
      @Override
      public void write(char[] cbuf, int off, int len) throws IOException {
        base.info(new String(cbuf,off,len));
      }

      @Override
      public void flush() throws IOException {

      }

      @Override
      public void close() throws IOException {

      }
    }));
  }

  @Override
  public void warn(Object message) {
    base.warning(message.toString());
  }

  @Override
  public void warn(Object message, Throwable t) {
    base.warning(message.toString());
    t.printStackTrace(new PrintWriter(new Writer() {
      @Override
      public void write(char[] cbuf, int off, int len) throws IOException {
        base.warning(new String(cbuf,off,len));
      }

      @Override
      public void flush() throws IOException {

      }

      @Override
      public void close() throws IOException {

      }
    }));
  }

  @Override
  public boolean isDebugEnabled() {
    return base.isLoggable(Level.FINEST);
  }

  @Override
  public boolean isInfoEnabled() {
    return base.isLoggable(Level.INFO);
  }

  @Override
  public void setLevel(LogLevel level) {
    base.setLevel(levelToBase(level));
  }

  @Override
  public LogLevel getLevel() {
    return baseToLevel(base.getLevel());
  }

  @Override
  public String getName() {
    return base.getName();
  }
  
  Level levelToBase(LogLevel level) {
    switch(level.getLevel()) {
      case 5:
        return Level.FINEST; //debug
      case 4:
        return Level.INFO;
      default:
        return Level.OFF;
    }
  }
  
  LogLevel baseToLevel(Level level) {
    int iLevel = level.intValue();
    if (Level.FINEST.intValue() == iLevel) return LogLevels.DEBUG;
    if (Level.INFO.intValue() == iLevel) return LogLevels.INFO;
    return new LogLevel() {
      @Override
      public int getLevel() {
        return 0;
      }

      @Override
      public boolean isInfo() {
        return false;
      }
    };
  }
}
