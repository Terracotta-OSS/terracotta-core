/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
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
