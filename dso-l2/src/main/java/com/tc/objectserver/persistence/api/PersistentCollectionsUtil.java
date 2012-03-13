/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.api;

import com.tc.object.ObjectID;
import com.tc.objectserver.core.api.ManagedObjectState;
import com.tc.objectserver.managedobject.ManagedObjectStateStaticConfig;
import com.tc.objectserver.persistence.db.PersistableCollection;
import com.tc.objectserver.persistence.db.PersistableCollectionFactory;

public class PersistentCollectionsUtil {

  public static boolean isPersistableCollectionType(final byte type) {
    if (ManagedObjectStateStaticConfig.SERVER_MAP.getStateObjectType() == type) { return true; }
    switch (type) {
      case ManagedObjectState.MAP_TYPE:
      case ManagedObjectState.PARTIAL_MAP_TYPE:
      case ManagedObjectState.SET_TYPE:
        return true;
      default:
        return false;
    }
  }

  public static boolean isEvictableMapType(final byte type) {
    if (ManagedObjectStateStaticConfig.SERVER_MAP.getStateObjectType() == type) { return true; }
    return false;
  }

  public static PersistableCollection createPersistableCollection(final ObjectID id,
                                                                  final PersistableCollectionFactory collectionFactory,
                                                                  final byte type) {
    if (ManagedObjectStateStaticConfig.SERVER_MAP.getStateObjectType() == type) { return (PersistableCollection) collectionFactory
        .createPersistentMap(id); }

    switch (type) {
      case ManagedObjectState.MAP_TYPE:
      case ManagedObjectState.PARTIAL_MAP_TYPE:
        return (PersistableCollection) collectionFactory.createPersistentMap(id);
      case ManagedObjectState.SET_TYPE:
        return (PersistableCollection) collectionFactory.createPersistentSet(id);
      default:
        return null;
    }
  }

}
