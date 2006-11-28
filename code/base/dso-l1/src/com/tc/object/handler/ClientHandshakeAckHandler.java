/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.object.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.async.api.EventHandlerException;
import com.tc.object.msg.ClientHandshakeAckMessage;

public class ClientHandshakeAckHandler extends AbstractEventHandler {

  public void handleEvent(EventContext context) throws EventHandlerException {
    if (true) throw new AssertionError("Don't use me.");
    if (!(context instanceof ClientHandshakeAckMessage)) throw new EventHandlerException("Unknown context type: "
                                                                                         + context);
  }

}
