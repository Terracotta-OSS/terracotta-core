/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.exception.TCRuntimeException;
import com.tc.io.serializer.api.Serializer;
import com.tc.object.ObjectID;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.core.api.ManagedObjectState;
import com.tc.objectserver.persistence.ManagedObjectPersistor;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class ManagedObjectSerializer implements Serializer {
  private final ManagedObjectStateSerializer serializer;
  private final ManagedObjectPersistor persistor;

  public ManagedObjectSerializer(ManagedObjectStateSerializer serializer, ManagedObjectPersistor persistor) {
    this.serializer = serializer;
    this.persistor = persistor;
  }

  public void serializeTo(final Object mo, final ObjectOutput out) {
    try {
      if (mo instanceof ManagedObject) {
        ((ManagedObject)mo).serializeTo(out, serializer);
      } else {
        throw new IllegalArgumentException("Trying to serialize a non-ManagedObject " + mo);
      }
    } catch (final IOException e) {
      throw new TCRuntimeException(e);
    }
  }

  public Object deserializeFrom(final ObjectInput in) {
    try {
      // read data
      final long version = in.readLong();
      final ObjectID id = new ObjectID(in.readLong());
      final ManagedObjectState state = (ManagedObjectState) this.serializer.deserializeFrom(in);

      // populate managed object...
      final ManagedObjectImpl rv = new ManagedObjectImpl(id, persistor);
      rv.setDeserializedState(version, state);
      return rv;
    } catch (final Exception e) {
      throw new TCRuntimeException(e);
    }
  }

  public byte getSerializerID() {
    return MANAGED_OBJECT;
  }
}
