/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.object;

import com.tc.text.PrettyPrinter;

public class NullObjectRequestMonitor implements ObjectRequestMonitor {

  public void notifyObjectRequest(ObjectRequestContext ctxt) {
    return;
  }

  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    return out;
  }

}
