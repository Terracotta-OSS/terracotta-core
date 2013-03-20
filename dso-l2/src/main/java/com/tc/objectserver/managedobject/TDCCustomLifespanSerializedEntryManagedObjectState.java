/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import java.io.IOException;
import java.io.ObjectInput;

public class TDCCustomLifespanSerializedEntryManagedObjectState extends TDCSerializedEntryManagedObjectState {

  public TDCCustomLifespanSerializedEntryManagedObjectState(final long classID) {
    super(classID);
  }

  @Override
  protected boolean basicEquals(final AbstractManagedObjectState o) {
    final TDCCustomLifespanSerializedEntryManagedObjectState other = (TDCCustomLifespanSerializedEntryManagedObjectState) o;

    return super.basicEquals(o);
  }

  @Override
  public byte getType() {
    return TDC_CUSTOM_LIFESPAN_SERIALIZED_ENTRY;
  }

  static TDCSerializedEntryManagedObjectState readFrom(final ObjectInput in) throws IOException {
    final TDCCustomLifespanSerializedEntryManagedObjectState state = new TDCCustomLifespanSerializedEntryManagedObjectState(
                                                                                                                            in.readLong());
    state.readFromInternal(in);
    return state;
  }
}
