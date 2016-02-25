/*
 *
 *  The contents of this file are subject to the Terracotta  License Version
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

import java.io.File;

/**
 * @author Mathoeu Carbou
 */
class Slf4jTCLogging implements DelegatingTCLogger {

  private static DelegatingTCLogger delegate = new Slf4jTCLogging();

  public static DelegatingTCLogger getDelegate() {
    return delegate;
  }

  private final TCLogger console = new Slf4jTCLogger(TCLogging.CONSOLE_LOGGER_NAME);
  private final TCLogger operatorEventLogger = new Slf4jTCLogger(TCLogging.OPERATOR_EVENT_LOGGER_NAME);

  private Slf4jTCLogging() {
  }

  @Override
  public void closeFileAppender() {

  }

  @Override
  public void setLogDirectory(File theDirectory, int processType) {

  }

  @Override
  public void disableLocking() {

  }

  @Override
  public TCLogger newLogger(String name) {
    return new Slf4jTCLogger(name);
  }

  @Override
  public TCLogger getConsoleLogger() {
    return console;
  }

  @Override
  public TCLogger getOperatorEventLogger() {
    return operatorEventLogger;
  }
}
