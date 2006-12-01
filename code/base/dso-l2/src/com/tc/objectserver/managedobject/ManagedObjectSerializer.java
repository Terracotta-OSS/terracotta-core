/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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

  public ManagedObjectSerializer(ManagedObjectStateSerializer serializer) {
    this.serializer = serializer;
  }

  public void serializeTo(Object mo, ObjectOutput out) {
    try {
      if (!(mo instanceof ManagedObjectImpl)) {
        //
        throw new AssertionError("Attempt to serialize an unknown type: " + mo);
      }
      ManagedObjectImpl moi = (ManagedObjectImpl) mo;
      out.writeLong(moi.version);
      out.writeLong(moi.id.toLong());
      serializer.serializeTo(moi.state, out);
    } catch (IOException e) {
      throw new TCRuntimeException(e);
    }
  }

  public Object deserializeFrom(ObjectInput in) {
    try {
      // read data
      long version = in.readLong();
      ObjectID id = new ObjectID(in.readLong());
      ManagedObjectState state = (ManagedObjectState) serializer.deserializeFrom(in);

      // populate managed object...
      ManagedObjectImpl rv = new ManagedObjectImpl(id);
      rv.version = version;
      rv.state = state;
      rv.setIsDirty(false);
      rv.setBasicIsNew(false);
      return rv;
    } catch (Exception e) {
      throw new TCRuntimeException(e);
    }
  }

  public byte getSerializerID() {
    return MANAGED_OBJECT;
  }
}
