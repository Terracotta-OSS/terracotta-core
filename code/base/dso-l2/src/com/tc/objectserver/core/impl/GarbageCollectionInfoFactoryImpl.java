/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.core.impl;

import com.tc.objectserver.core.api.GarbageCollectionInfo;
import com.tc.objectserver.core.api.GarbageCollectionInfoFactory;

public class GarbageCollectionInfoFactoryImpl implements GarbageCollectionInfoFactory {

  public GarbageCollectionInfo newInstance(int iteration) {
    return new GarbageCollectionInfoImpl(iteration);
  }

}
