/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.objectserver.api.ObjectManagerStatsListener;

public class NullObjectManagerStatsListener implements ObjectManagerStatsListener {

  public NullObjectManagerStatsListener() {
    //
  }

  public void cacheHit() {
    //
  }

  public void cacheMiss() {
    //
  }

  public void newObjectCreated() {
    //
  }

}
