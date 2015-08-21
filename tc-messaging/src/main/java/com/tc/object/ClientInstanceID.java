package com.tc.object;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCSerializable;

import java.io.IOException;

/**
 * @author jdis
 * Used to identify a specific acquire result of an Entity, on a client.  Note that this ID isn't a global value, but must be
 * considered within the context of a specific Entity type on a specific client.
 */
public class ClientInstanceID implements TCSerializable<ClientInstanceID> {
  // We create this NULL_ID for cases such as CREATE/DELETE where an instance ID is still needed for the request but ideally
  // this would be removed in the future as those calls will never have instance IDs.
  public static final ClientInstanceID NULL_ID = new ClientInstanceID(0);

  private final long id;
  
  public ClientInstanceID(long id) {
    this.id = id;
  }

  public long getID() {
    return this.id;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final ClientInstanceID that = (ClientInstanceID) o;

    return (this.id == that.id);
  }

  @Override
  public int hashCode() {
    return (int)this.id;
  }

  @Override
  public void serializeTo(TCByteBufferOutput serialOutput) {
    serialOutput.writeLong(this.id);
  }

  @Override
  public ClientInstanceID deserializeFrom(TCByteBufferInput serialInput) throws IOException {
    return readFrom(serialInput);
  }

  public static ClientInstanceID readFrom(TCByteBufferInput serialInput) throws IOException {
    return new ClientInstanceID(serialInput.readLong());
  }

  @Override
  public String toString() {
    return "ClientInstanceID(" + this.id + ")";
  }
}
