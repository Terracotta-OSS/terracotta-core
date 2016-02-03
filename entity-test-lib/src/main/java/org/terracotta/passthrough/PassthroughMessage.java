/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Entity API.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
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
    LOCK_TRY_ACQUIRE,
    LOCK_RELEASE,
    LOCK_RESTORE,
    RECONNECT,
    SYNC_ENTITY_START,
    SYNC_ENTITY_END,
    SYNC_ENTITY_KEY_START,
    SYNC_ENTITY_KEY_END,
    SYNC_ENTITY_PAYLOAD,
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
