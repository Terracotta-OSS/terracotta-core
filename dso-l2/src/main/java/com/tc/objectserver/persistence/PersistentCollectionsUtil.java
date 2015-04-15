/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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