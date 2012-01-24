/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.api;

import com.tc.object.ObjectID;
import com.tc.objectserver.core.api.ManagedObjectState;
import com.tc.objectserver.persistence.db.PersistableCollection;
import com.tc.objectserver.persistence.db.PersistableCollectionFactory;

public class PersistentCollectionsUtil {

  public static boolean isPersistableCollectionType(final byte type) {
    switch (type) {
      case ManagedObjectState.MAP_TYPE:
      case ManagedObjectState.PARTIAL_MAP_TYPE:
      case ManagedObjectState.SET_TYPE:
      case ManagedObjectState.CONCURRENT_DISTRIBUTED_MAP_TYPE:
      case ManagedObjectState.CONCURRENT_DISTRIBUTED_SERVER_MAP_TYPE:
        return true;
      default:
        return false;
    }
  }

  public static boolean isEvictableMapType(final byte type) {
    switch (type) {
      case ManagedObjectState.CONCURRENT_DISTRIBUTED_SERVER_MAP_TYPE:
        return true;
      default:
        return false;
    }
  }

  public static PersistableCollection createPersistableCollection(final ObjectID id,
                                                                  final PersistableCollectionFactory collectionFactory,
                                                                  final byte type) {
    switch (type) {
      case ManagedObjectState.MAP_TYPE:
      case ManagedObjectState.PARTIAL_MAP_TYPE:
      case ManagedObjectState.CONCURRENT_DISTRIBUTED_MAP_TYPE:
      case ManagedObjectState.CONCURRENT_DISTRIBUTED_SERVER_MAP_TYPE:
        return (PersistableCollection) collectionFactory.createPersistentMap(id);
      case ManagedObjectState.SET_TYPE:
        return (PersistableCollection) collectionFactory.createPersistentSet(id);
      default:
        return null;
    }
  }

}
