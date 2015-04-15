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
package com.tc.operatorevent;

import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.operatorevent.TerracottaOperatorEvent.EventLevel;

public class TerracottaOperatorEventCallbackLogger implements TerracottaOperatorEventCallback {

  private final TCLogger logger = CustomerLogging.getOperatorEventLogger();

  @Override
  public void logOperatorEvent(TerracottaOperatorEvent event) {
    EventLevel eventType = event.getEventLevel();
    switch (eventType) {
      case INFO:
        this.logger.info("NODE : " + event.getNodeName() + " Subsystem: " + event.getEventSubsystem() + " EventType: "
                         + event.getEventType() + " Message: " + event.getEventMessage());
        break;
      case WARN:
        this.logger.warn("NODE : " + event.getNodeName() + " Subsystem: " + event.getEventSubsystem() + " EventType: "
                         + event.getEventType() + " Message: " + event.getEventMessage());
        break;
      case DEBUG:
        this.logger.debug("NODE : " + event.getNodeName() + " Subsystem: " + event.getEventSubsystem() + " EventType: "
                          + event.getEventType() + " Message: " + event.getEventMessage());
        break;
      case ERROR:
        this.logger.error("NODE : " + event.getNodeName() + " Subsystem: " + event.getEventSubsystem() + " EventType: "
                          + event.getEventType() + " Message: " + event.getEventMessage());
        break;
      case CRITICAL:
        this.logger.fatal("NODE : " + event.getNodeName() + " Subsystem: " + event.getEventSubsystem() + " EventType: "
                          + event.getEventType() + " Message: " + event.getEventMessage());
        break;
      default:
        throw new RuntimeException("invalid event type: " + eventType);
    }
  }

}
