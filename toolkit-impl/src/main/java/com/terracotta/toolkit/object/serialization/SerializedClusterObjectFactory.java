/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.object.serialization;

import org.terracotta.toolkit.object.serialization.NotSerializableRuntimeException;

import com.tc.net.GroupID;

public interface SerializedClusterObjectFactory {

  /**
   * Creates serialized version of the object, that can be stored in the cluster, in the stripe denoted by gid
   * 
   * @throws NotSerializableRuntimeException if value is not serializable
   */
  SerializedClusterObject createSerializedClusterObject(Object value, GroupID gid)
      throws NotSerializableRuntimeException;

  /**
   * Creates serialized map value, that can be stored in the cluster, in the stripe denoted by gid
   */
  <T> SerializedMapValue<T> createSerializedMapValue(SerializedMapValueParameters<T> params, GroupID gid);
}
