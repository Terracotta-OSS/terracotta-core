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

import com.tc.logging.TCLogger;
import com.tc.logging.TCLoggingService;
import java.net.URI;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class TCClientLogging implements TCLoggingService {

  @Override
  public TCLogger getLogger(Class<?> className) {
    return new TCLoggerClientSLF4J(LoggerFactory.getLogger("class." + className));
  }

  @Override
  public TCLogger getLogger(String className) {
    return new TCLoggerClientSLF4J(LoggerFactory.getLogger("class." + className));
  }

  @Override
  public TCLogger getTestingLogger(String name) {
    return new TCLoggerClientSLF4J(LoggerFactory.getLogger("testing.logger." + name));
  }

  @Override
  public TCLogger getConsoleLogger() {
    return new TCLoggerClientSLF4J(LoggerFactory.getLogger("console.logger"));
  }

  @Override
  public TCLogger getOperatorEventLogger() {
    return new TCLoggerClientSLF4J(LoggerFactory.getLogger("operator.events"));
  }

  @Override
  public TCLogger getDumpLogger() {
    return new TCLoggerClientSLF4J(LoggerFactory.getLogger("dump.logger"));
  }

  @Override
  public TCLogger getCustomerLogger(String name) {
    return new TCLoggerClientSLF4J(LoggerFactory.getLogger("customer.logger." + name));
  }

  @Override
  public void setLogLocationAndType(URI location, int processType) {
  }
}
