/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.simulator.app;

public interface Application extends Runnable {
  public String getApplicationId();
  public boolean interpretResult(Object result);
}
