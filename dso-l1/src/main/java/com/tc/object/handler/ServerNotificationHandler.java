package com.tc.object.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.async.api.EventHandlerException;
import com.tc.object.ServerInterestListenerManager;
import com.tc.object.msg.EvictionInterestMessage;
import com.tc.object.msg.ExpirationInterestMessage;
import com.tc.util.Assert;

/**
 * @author Eugene Shelestovich
 */
public class ServerNotificationHandler extends AbstractEventHandler {

  private final ServerInterestListenerManager manager;

  public ServerNotificationHandler(final ServerInterestListenerManager manager) {
    this.manager = manager;
  }

  @Override
  public void handleEvent(final EventContext context) throws EventHandlerException {
    if (context instanceof EvictionInterestMessage) {

    } else if (context instanceof ExpirationInterestMessage) {

    } else {
      Assert.fail("Unknown event type " + context.getClass().getName());
    }
  }
}
