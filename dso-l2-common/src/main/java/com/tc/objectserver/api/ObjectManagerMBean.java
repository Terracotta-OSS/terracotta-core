/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.api;

import com.tc.object.ObjectID;
import com.tc.objectserver.mgmt.ManagedObjectFacade;

import java.util.Iterator;

/**
 * Management interface for ObjectManager
 */
public interface ObjectManagerMBean {

  Iterator getRoots();

  Iterator getRootNames();

  ObjectID lookupRootID(String name);

  /**
   * Returns a object facade instance. This call does not checkout the object, transactions can be applied to the
   * underlying managed object whilst clients are holding the facade <br>
   * <br>
   * NOTE: Getting an object facade is both "unsafe" and expensive. They are unsafe in that no locking occurs, and
   * expensive since there is a complete data copy. At the moment these facades are created to enable the root/object
   * browser in the Admin tool (ie. low volume).
   */
  public ManagedObjectFacade lookupFacade(ObjectID id, int limit) throws NoSuchObjectException;

  int getLiveObjectCount();
  
  int getCachedObjectCount();
}
