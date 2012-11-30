/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.events;

import org.terracotta.toolkit.monitoring.OperatorEventLevel;

import com.tc.operatorevent.TerracottaOperatorEvent.EventSubsystem;
import com.tc.operatorevent.TerracottaOperatorEvent.EventType;
import com.tc.platform.PlatformService;

public class OperatorEventUtil {

  private static EventType translateOperatorEventLevel(OperatorEventLevel level) {
    switch (level) {
      case INFO: {
        return EventType.INFO;
      }
      case WARN: {
        return EventType.WARN;
      }
      case DEBUG: {
        return EventType.DEBUG;
      }
      case ERROR: {
        return EventType.ERROR;
      }
      case CRITICAL: {
        return EventType.CRITICAL;
      }
    }
    // don't do this as the "default" in the switch block so the compiler can catch errors
    throw new AssertionError("unknown OperatorEventLevel " + level);
  }

  public static void fireOperatorEvent(PlatformService platformService, OperatorEventLevel level,
                                       String applicationName, String eventMessage) {
    String message = applicationName + ": " + eventMessage;
    platformService.fireOperatorEvent(translateOperatorEventLevel(level), EventSubsystem.APPLICATION, message);

  }
}
