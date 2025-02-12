/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory class for obtaining Logger instances.
 * 
 * @author teck
 */
public class TCLogging {

  public static final String CONSOLE_LOGGER_NAME = "org.terracotta.console";
  public static final String SILENT_LOGGER_NAME = "org.terracotta.silent";
  public static final String DUMP_LOGGER_NAME = "org.terracotta.dump";

  private static final Logger CONSOLE_LOGGER = LoggerFactory.getLogger(CONSOLE_LOGGER_NAME);
  private static final Logger SILENT_LOGGER = LoggerFactory.getLogger(SILENT_LOGGER_NAME);
  private static final Logger DUMP_LOGGER = new DumpLogger(LoggerFactory.getLogger(DUMP_LOGGER_NAME));

  public static Logger getConsoleLogger() {
    return CONSOLE_LOGGER;
  }

  public static Logger getDumpLogger() {
    return DUMP_LOGGER;
  }

  public static Logger getSilentLogger() {
    return SILENT_LOGGER;
  }
  
}
