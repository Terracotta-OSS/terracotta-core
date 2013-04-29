package com.tc.object.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.object.ServerEventListenerManager;
import com.tc.object.msg.ServerEventMessage;

/**
 * @author Eugene Shelestovich
 */
public class ServerNotificationHandler extends AbstractEventHandler {

  private final ServerEventListenerManager manager;

  public ServerNotificationHandler(final ServerEventListenerManager manager) {
    this.manager = manager;
  }

  @Override
  public void handleEvent(final EventContext context) {
    if (context instanceof ServerEventMessage) {
      final ServerEventMessage message = (ServerEventMessage)context;
      manager.dispatch(message);
    } else {
      throw new AssertionError("Unknown event type: " + context.getClass().getName());
    }
  }
}
