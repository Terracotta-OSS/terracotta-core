/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.simulator.app;

public interface Application extends Runnable {
  public String getApplicationId();
  public boolean interpretResult(Object result);
}
