/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.object.lockmanager.api;

public interface WaitTimerCallback {
  
  public void waitTimeout(Object callbackObject);

}
