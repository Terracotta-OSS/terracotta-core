/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.object.msg.JMXMessage;
import com.tc.objectserver.DSOApplicationEvents;

public class JMXEventsHandler extends AbstractEventHandler {

  private final DSOApplicationEvents appEvents;

  public JMXEventsHandler(DSOApplicationEvents appEvents) {
    this.appEvents = appEvents;
  }

  public void handleEvent(EventContext context) {
    if (context instanceof JMXMessage) {
      appEvents.addMessage((JMXMessage) context);
    } else {
      throw new AssertionError("Unknown event type: " + context);
    }
  }

}
