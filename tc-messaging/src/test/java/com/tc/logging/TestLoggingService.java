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
  public TCLogger getLogger(final String name) {
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
