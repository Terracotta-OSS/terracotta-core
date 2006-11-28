/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
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
