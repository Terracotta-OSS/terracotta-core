package com.tc.object.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.object.ServerInterestListenerManager;
import com.tc.object.msg.ServerInterestMessage;

/**
 * @author Eugene Shelestovich
 */
public class ServerNotificationHandler extends AbstractEventHandler {

  private final ServerInterestListenerManager manager;

  public ServerNotificationHandler(final ServerInterestListenerManager manager) {
    this.manager = manager;
  }

  @Override
  public void handleEvent(final EventContext context) {
    if (context instanceof ServerInterestMessage) {
      final ServerInterestMessage message = (ServerInterestMessage)context;
      manager.dispatchInterest(message);
    } else {
      throw new AssertionError("Unknown event type: " + context.getClass().getName());
    }
  }
}
