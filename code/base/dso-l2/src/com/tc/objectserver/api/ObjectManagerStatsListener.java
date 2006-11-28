/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.objectserver.api;

public interface ObjectManagerStatsListener {
  
  public void cacheHit();

  public void cacheMiss();
  
  public void newObjectCreated();
  
}
