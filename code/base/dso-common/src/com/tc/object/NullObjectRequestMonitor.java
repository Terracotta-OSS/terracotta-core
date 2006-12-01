/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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
