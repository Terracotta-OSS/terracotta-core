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
 * Factory class for obtaining Logger instances.
 * 
 * @author teck
 */
public class TCLogging {

  public static final String CONSOLE_LOGGER_NAME = "org.terracotta.console";
  public static final String DUMP_LOGGER_NAME = "org.terracotta.dump";

  private static final Logger CONSOLE_LOGGER = LoggerFactory.getLogger(CONSOLE_LOGGER_NAME);
  private static final Logger DUMP_LOGGER = new DumpLogger(LoggerFactory.getLogger(DUMP_LOGGER_NAME));

  public static Logger getConsoleLogger() {
    return CONSOLE_LOGGER;
  }

  public static Logger getDumpLogger() {
    return DUMP_LOGGER;
  }
  
}
