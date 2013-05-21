/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.exception.TCRuntimeException;
import com.tc.io.serializer.api.Serializer;
import com.tc.objectserver.core.api.ManagedObjectState;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class ManagedObjectStateSerializer implements Serializer {

  @Override
  public void serializeTo(Object o, ObjectOutput out) throws IOException {
    if (!(o instanceof ManagedObjectState)) throw new AssertionError("Attempt to serialize an unknown type: " + o);
    ManagedObjectState mo = (ManagedObjectState) o;
    out.writeByte(mo.getType());
    mo.writeTo(out);
  }

  @Override
  public Object deserializeFrom(ObjectInput in) throws IOException {
    byte type = in.readByte();
    return getStateFactory().readManagedObjectStateFrom(in, type);
  }

  @Override
  public byte getSerializerID() {
    return MANAGED_OBJECT_STATE;
  }

  private ManagedObjectStateFactory getStateFactory() {
    return ManagedObjectStateFactory.getInstance();
  }

}
