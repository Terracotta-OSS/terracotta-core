/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.object.event.DmiEventContext;
import com.tc.object.event.DmiManager;
import com.tc.util.Assert;

public class DmiHandler extends AbstractEventHandler {

  private final DmiManager dmiMgr;

  public DmiHandler(DmiManager dmiMgr) {
    Assert.pre(dmiMgr != null);
    this.dmiMgr = dmiMgr;
  }

  public void handleEvent(EventContext context) {
    DmiEventContext dmiEvent = (DmiEventContext) context;
    dmiMgr.invoke(dmiEvent.getDmc());
  }

}
