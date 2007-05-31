/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.delivery;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.async.api.EventHandlerException;

public class OOOEventHandler extends AbstractEventHandler {

  public void handleEvent(EventContext context) throws EventHandlerException {
    if (!(context instanceof StateMachineRunner)) throw new EventHandlerException("Unexpected EventContext: "
                                                                                  + ((context == null) ? "null"
                                                                                      : context.getClass().getName()));
    StateMachineRunner smr = (StateMachineRunner) context;
    smr.run();
  }

}
