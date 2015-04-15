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
