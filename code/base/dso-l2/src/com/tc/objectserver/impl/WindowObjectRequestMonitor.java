/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.objectserver.api.ObjectRequestMonitor;
import com.tc.objectserver.context.ManagedObjectRequestContext;
import com.tc.text.PrettyPrinter;
import com.tc.util.WindowUtil;

public class WindowObjectRequestMonitor implements ObjectRequestMonitor {

  private final WindowUtil window;
  
  public WindowObjectRequestMonitor(int size) {
    window = new WindowUtil(size); 
  }
  
  public void notifyObjectRequest(ManagedObjectRequestContext ctxt) {
    window.add(ctxt);
  }

  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    out.println("WindowObjectRequestMonitor");
    out.duplicateAndIndent().indent().visit(window);
    return out;
  }

}
