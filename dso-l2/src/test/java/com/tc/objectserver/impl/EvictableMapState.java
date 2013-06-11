/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.objectserver.api.EvictableMap;
import com.tc.objectserver.core.api.ManagedObjectState;
import com.tc.objectserver.managedobject.ManagedObjectStateStaticConfig;

/**
 *
 * @author mscott
 */
public abstract class EvictableMapState implements ManagedObjectState, EvictableMap  {

  @Override
  public byte getType() {
    return ManagedObjectStateStaticConfig.SERVER_MAP.getStateObjectType();
  }
}
