/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.async.api.EventHandler;
import com.tc.invalidation.InvalidationsProcessor;
import com.tc.object.msg.InvalidateObjectsMessage;

public class ReceiveInvalidationHandler extends AbstractEventHandler implements EventHandler {

  private final InvalidationsProcessor invalidationsProcessor;

  public ReceiveInvalidationHandler(InvalidationsProcessor invalidationsProcessor) {
    this.invalidationsProcessor = invalidationsProcessor;
  }

  @Override
  public void handleEvent(EventContext context) {
    InvalidateObjectsMessage invalidationContext = (InvalidateObjectsMessage) context;
    invalidationsProcessor.processInvalidations(invalidationContext.getObjectIDsToInvalidate());
  }
}
