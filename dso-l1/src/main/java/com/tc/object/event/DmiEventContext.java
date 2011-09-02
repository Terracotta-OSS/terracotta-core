/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.event;

import com.tc.async.api.EventContext;
import com.tcclient.object.DistributedMethodCall;

public class DmiEventContext implements EventContext {

  private final DistributedMethodCall dmc;

  public DmiEventContext(DistributedMethodCall dmc) {
    this.dmc = dmc;
  }

  public DistributedMethodCall getDmc() {
    return dmc;
  }

}
