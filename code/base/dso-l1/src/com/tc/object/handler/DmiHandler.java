/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.object.dmi.DmiDescriptor;
import com.tc.object.event.DmiManager;
import com.tc.util.Assert;

public class DmiHandler extends AbstractEventHandler {

  private final DmiManager dmiMgr;

  public DmiHandler(DmiManager dmiMgr) {
    Assert.pre(dmiMgr != null);
    this.dmiMgr = dmiMgr;
  }

  public void handleEvent(EventContext context) {
    DmiDescriptor dd = (DmiDescriptor) context;
    dmiMgr.invoke(dd);
  }

}
