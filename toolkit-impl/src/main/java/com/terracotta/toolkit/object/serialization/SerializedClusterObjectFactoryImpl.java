/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.object.serialization;

import org.terracotta.toolkit.object.serialization.NotSerializableRuntimeException;

import com.tc.net.GroupID;
import com.tc.object.bytecode.ManagerUtil;

import java.io.Serializable;

public class SerializedClusterObjectFactoryImpl implements SerializedClusterObjectFactory {

  private final SerializationStrategy strategy;

  public SerializedClusterObjectFactoryImpl(SerializationStrategy strategy) {
    this.strategy = strategy;
  }

  public SerializedClusterObject createSerializedClusterObject(final Object value, final GroupID gid)
      throws NotSerializableRuntimeException {
    SerializedClusterObject clusterObject;
    clusterObject = new SerializedClusterObjectImpl(value, strategy.serialize(value, false));
    ManagerUtil.lookupOrCreate(clusterObject, gid);
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
    ManagerUtil.lookupOrCreate(serializedMapValue, gid);
    return serializedMapValue;
  }

}