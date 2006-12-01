/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.runner;

public interface TestDriver {
  
  public String[] getTestParameters();
  
  public void notifyError(Throwable t);

}
