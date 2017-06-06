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
import org.slf4j.LoggerFactory;

import java.util.ServiceLoader;

/**
 * Factory class for obtaining TCLogger instances.
 * 
 * @author teck
 */
public class TCLogging {

  private static final Logger DUMP_LOGGER = LoggerFactory.getLogger("com.tc.dumper.dump");
  private static final Logger CONSOLE_LOGGER = LoggerFactory.getLogger("com.terracottatech.console");

  private static TCLoggingService  service;
  
  static {
    service = new NullTCLoggingService();
    ServiceLoader<TCLoggingService> loader = ServiceLoader.load(TCLoggingService.class);
    for (TCLoggingService possible : loader) {
      service = possible;
    }
  }

  public static TCLogger getLogger(Class<?> clazz) {
    if (clazz == null) { throw new IllegalArgumentException("Class cannot be null"); }
    return getLogger(clazz.getName());
  }

  public static TCLogger getLogger(String name) {
    if (name == null) { throw new NullPointerException("Logger cannot be null"); }

    return service.getLogger(name);    
  }

  public static Logger getConsoleLogger() {
    return CONSOLE_LOGGER;
  }

  public static Logger getDumpLogger() {
    return DUMP_LOGGER;
  }
  
  public static void setLogLocationAndType(java.net.URI logLocation) {
    service.setLogLocationAndType(logLocation);
  }
  
  public static void setLoggingService(TCLoggingService service) {
   TCLogging.service = service;
  }
 //  this is only a way to get to the actual logging implementation.  Normally there should be no need to access it.
  public static TCLoggingService getLoggingService() {
    return TCLogging.service;
  }
}
