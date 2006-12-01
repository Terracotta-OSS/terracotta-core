/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.api;

public interface ObjectManagerStatsListener {
  
  public void cacheHit();

  public void cacheMiss();
  
  public void newObjectCreated();
  
}
