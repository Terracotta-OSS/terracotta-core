package org.terracotta.passthrough;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;


/**
 * The top-level abstract class of all messages passed through the inter-thread "wire" between the "server" and "client".
 */
public abstract class PassthroughMessage {
  public enum Type {
    FETCH_ENTITY,
    RELEASE_ENTITY,
    DOES_ENTITY_EXIST,
    CREATE_ENTITY,
    DESTROY_ENTITY,
    INVOKE_ON_SERVER,
    ACK_FROM_SERVER,
    COMPLETE_FROM_SERVER,
    INVOKE_ON_CLIENT,
    LOCK_ACQUIRE,
    LOCK_RELEASE,
  }

  public static Type getType(DataInputStream input) throws IOException {
    int ordinal = input.readInt();
    return Type.values()[ordinal];
  }

  public final Type type;
  public final boolean shouldReplicateToPassives;
  public long transactionID;
  
  public PassthroughMessage(Type type, boolean shouldReplicateToPassives) {
    this.shouldReplicateToPassives = shouldReplicateToPassives;
    this.type = type;
  }

  public void setTransactionID(long transactionID) {
    this.transactionID = transactionID;
  }

  public byte[] asSerializedBytes() {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try {
      DataOutputStream output = new DataOutputStream(bytes);
      output.writeInt(this.type.ordinal());
      output.writeBoolean(this.shouldReplicateToPassives);
      output.writeLong(this.transactionID);
      this.populateStream(output);
      output.close();
    } catch (IOException e) { 
      // Can't happen with a byte array.
      Assert.unexpected(e);
    }
    return bytes.toByteArray();
  }
  
  protected abstract void populateStream(DataOutputStream output) throws IOException;
}
