/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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