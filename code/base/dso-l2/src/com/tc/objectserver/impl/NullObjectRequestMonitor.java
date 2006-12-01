/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.objectserver.api.ObjectRequestMonitor;
import com.tc.objectserver.context.ManagedObjectRequestContext;
import com.tc.text.PrettyPrinter;

public class NullObjectRequestMonitor implements ObjectRequestMonitor {

  public void notifyObjectRequest(ManagedObjectRequestContext ctxt) {
    return;
  }

  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    return out.print("Null ObjectRequestMonitor");
  }

}
