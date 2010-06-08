/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.operatorevent;

import com.tc.logging.TCLogger;
import com.tc.operatorevent.TerracottaOperatorEvent.EventType;

public class TerracottaOperatorEventCallbackLogger implements TerracottaOperatorEventCallback {

  private final TCLogger logger;

  public TerracottaOperatorEventCallbackLogger(TCLogger logger) {
    this.logger = logger;
  }

  public void logOperatorEvent(TerracottaOperatorEvent event) {
    EventType eventType = event.getEventType();
    switch (eventType) {
      case INFO:
        this.logger.info("Operator Event: TYPE: " + eventType + " NODE : " + event.getNodeName() + " Subsystem: "
                         + event.getEventSubsystem() + " Message: " + event.getEventMessage());
        break;
      case WARN:
        this.logger.warn("Operator Event: TYPE: " + eventType + " NODE : " + event.getNodeName() + " Subsystem: "
                         + event.getEventSubsystem() + " Message: " + event.getEventMessage());
        break;
      case DEBUG:
        this.logger.debug("Operator Event: TYPE: " + eventType + " NODE : " + event.getNodeName() + " Subsystem: "
                          + event.getEventSubsystem() + " Message: " + event.getEventMessage());
        break;
      case ERROR:
        this.logger.error("Operator Event: TYPE: " + eventType + " NODE : " + event.getNodeName() + " Subsystem: "
                          + event.getEventSubsystem() + " Message: " + event.getEventMessage());
        break;
      case CRITICAL:
        this.logger.fatal("Operator Event: TYPE: " + eventType + " NODE : " + event.getNodeName() + " Subsystem: "
                          + event.getEventSubsystem() + " Message: " + event.getEventMessage());
        break;
      default:
        throw new RuntimeException("invalid event type: " + eventType);
    }
  }

}
