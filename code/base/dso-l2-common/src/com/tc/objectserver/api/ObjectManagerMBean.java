/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.api;

import com.tc.object.ObjectID;
import com.tc.objectserver.mgmt.ManagedObjectFacade;

import java.util.Iterator;

/**
 * Management interface for ObjectManager
 */
public interface ObjectManagerMBean {

  void addListener(ObjectManagerEventListener listener);

  Iterator getRoots();

  Iterator getRootNames();

  ObjectID lookupRootID(String name);

  GCStats[] getGarbageCollectorStats();

  /**
   * Returns a object facade instance. This call does not checkout the object, transactions can be applied to the
   * underlying managed object whilst clients are holding the facade <br>
   * <br>
   * NOTE: Getting an object facade is both "unsafe" and expensive. They are unsafe in that no locking occurs, and
   * expensive since there is a complete data copy. At the moment these facades are created to enable the root/object
   * browser in the Admin tool (ie. low volume).
   */
  public ManagedObjectFacade lookupFacade(ObjectID id, int limit) throws NoSuchObjectException;

}
