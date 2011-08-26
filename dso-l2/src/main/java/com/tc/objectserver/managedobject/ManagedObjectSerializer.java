/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.exception.TCRuntimeException;
import com.tc.io.serializer.api.Serializer;
import com.tc.object.ObjectID;
import com.tc.objectserver.core.api.ManagedObjectState;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class ManagedObjectSerializer implements Serializer {
  private final ManagedObjectStateSerializer serializer;

  public ManagedObjectSerializer(final ManagedObjectStateSerializer serializer) {
    this.serializer = serializer;
  }

  public void serializeTo(final Object mo, final ObjectOutput out) {
    try {
      if (!(mo instanceof ManagedObjectImpl)) {
        //
        throw new AssertionError("Attempt to serialize an unknown type: " + mo);
      }
      final ManagedObjectImpl moi = (ManagedObjectImpl) mo;
      out.writeLong(moi.getVersion());
      out.writeLong(moi.getObjectID().toLong());
      this.serializer.serializeTo(moi.getManagedObjectState(), out);
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
      final ManagedObjectImpl rv = new ManagedObjectImpl(id);
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
