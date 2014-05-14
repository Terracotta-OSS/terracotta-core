/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence;

import com.tc.objectserver.core.api.ManagedObjectState;
import com.tc.objectserver.managedobject.ManagedObjectStateStaticConfig;

public class PersistentCollectionsUtil {

  public static boolean isPersistableCollectionType(final byte type) {
    if (ManagedObjectStateStaticConfig.SERVER_MAP.getStateObjectType() == type) { return true; }
    switch (type) {
      case ManagedObjectState.MAP_TYPE:
      case ManagedObjectState.PARTIAL_MAP_TYPE:
      case ManagedObjectState.TOOLKIT_TYPE_ROOT_TYPE:
        return true;
      default:
        return false;
    }
  }

  public static boolean isNoReferenceObjectType(final byte type) {
    return type == ManagedObjectStateStaticConfig.SERIALIZED_CLUSTER_OBJECT.getStateObjectType();
  }

  public static boolean isEvictableMapType(final byte type) {
    if (ManagedObjectStateStaticConfig.SERVER_MAP.getStateObjectType() == type) { return true; }
    return false;
  }

}