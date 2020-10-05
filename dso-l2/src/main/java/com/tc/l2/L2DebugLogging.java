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
package com.tc.l2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tc.objectserver.impl.ThisServerNodeId;
import com.tc.properties.TCPropertiesImpl;

public abstract class L2DebugLogging {

  private static final String  L2_DEBUG_LOGGING_PROP_NAME = "l2.debug-logging";
  private static final boolean ENABLED                    = TCPropertiesImpl.getProperties()
                                                              .getBoolean(L2_DEBUG_LOGGING_PROP_NAME, false);

  static {
    Logger logger = LoggerFactory.getLogger(L2DebugLogging.class);
  }

  public static enum LogLevel {
    ERROR, WARN, INFO, DEBUG;
  }
  
  public static boolean isDebugLogging() {
    return ENABLED;
  }

  public static void log(Logger log, LogLevel level, String message, Throwable throwable) {
    if (!ENABLED) return;

    message = "[" + ThisServerNodeId.getThisServerNodeId() + "] " + message;
    switch (level) {
      case ERROR:
        if (throwable != null) {
          log.error(message, throwable);
        } else {
          log.error(message);
        }
        break;
      case WARN:
        if (throwable != null) {
          log.warn(message, throwable);
        } else {
          log.warn(message);
        }
        break;
      case INFO:
        if (throwable != null) {
          log.info(message, throwable);
        } else {
          log.info(message);
        }
        break;
      case DEBUG:
        if (throwable != null) {
          log.debug(message, throwable);
        } else {
          log.debug(message);
        }
        break;
      default:
        if (throwable != null) {
          log.error(message, throwable);
        } else {
          log.error(message);
        }
        break;
    }
  }
}
