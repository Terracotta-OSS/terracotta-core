package com.tc.object.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.object.ServerEventListenerManager;
import com.tc.object.context.ServerEventDeliveryContext;

/**
 * Process server events one by one.
 *
 * @author Eugene Shelestovich
 */
public class ServerEventDeliveryHandler extends AbstractEventHandler {

  private final ServerEventListenerManager manager;

  public ServerEventDeliveryHandler(final ServerEventListenerManager manager) { this.manager = manager; }

  @Override
  public void handleEvent(final EventContext ctx) {
    if (ctx instanceof ServerEventDeliveryContext) {
      final ServerEventDeliveryContext msg = (ServerEventDeliveryContext) ctx;

      manager.dispatch(msg.getEvent(), msg.getRemoteNode());
    } else {
      throw new AssertionError("Unknown event type: " + ctx.getClass().getName());
    }
  }
}
