/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.api;

import com.tc.object.ObjectID;

import java.util.Iterator;

/**
 * Management interface for ObjectManager
 */
public interface ObjectManagerMBean {

  Iterator getRoots();

  Iterator getRootNames();

  ObjectID lookupRootID(String name);

  int getLiveObjectCount();
}
