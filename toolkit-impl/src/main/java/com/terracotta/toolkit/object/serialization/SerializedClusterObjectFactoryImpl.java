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
import com.tc.platform.PlatformService;

import java.io.Serializable;

public class SerializedClusterObjectFactoryImpl implements SerializedClusterObjectFactory {

  private final SerializationStrategy strategy;
  private final PlatformService       platformService;

  public SerializedClusterObjectFactoryImpl(PlatformService platformService, SerializationStrategy strategy) {
    this.strategy = strategy;
    this.platformService = platformService;
  }

  @Override
  public SerializedClusterObject createSerializedClusterObject(final Object value, final GroupID gid)
      throws NotSerializableRuntimeException {
    SerializedClusterObject clusterObject;
    clusterObject = new SerializedClusterObjectImpl(value, strategy.serialize(value, false));
    platformService.lookupOrCreate(clusterObject, gid);
    return clusterObject;
  }

  @Override
  public SerializedMapValue createSerializedMapValue(SerializedMapValueParameters params, GroupID gid) {
    SerializedMapValue serializedMapValue;
    if (params.isCustomLifespan()) {
      serializedMapValue = new CustomLifespanSerializedMapValue<Serializable>(params);
    } else {
      serializedMapValue = new SerializedMapValue<Serializable>(params);
    }
    platformService.lookupOrCreate(serializedMapValue, gid);
    return serializedMapValue;
  }

}