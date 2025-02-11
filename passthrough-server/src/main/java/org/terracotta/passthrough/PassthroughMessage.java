/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
    RECONFIGURE_ENTITY,
    DESTROY_ENTITY,
    INVOKE_ON_SERVER,
    ACK_FROM_SERVER,
    COMPLETE_FROM_SERVER,
    EXCEPTION_FROM_SERVER,
    RETIRE_FROM_SERVER,
    INVOKE_ON_CLIENT,
    RECONNECT,
    SYNC_ENTITY_START,
    SYNC_ENTITY_END,
    SYNC_ENTITY_KEY_START,
    SYNC_ENTITY_KEY_END,
    SYNC_ENTITY_PAYLOAD,
    UNEXPECTED_RELEASE,
    DROP_LOCK,
    MONITOR_MESSAGE,
    MONITOR_EXCEPTION,
  }

  public static Type getType(DataInputStream input) throws IOException {
    int ordinal = input.readInt();
    return Type.values()[ordinal];
  }

  public final Type type;
  public final boolean shouldReplicateToPassives;
  public long transactionID;
  public long oldestTransactionID;
  
  public PassthroughMessage(Type type, boolean shouldReplicateToPassives) {
    this.shouldReplicateToPassives = shouldReplicateToPassives;
    this.type = type;
  }

  public void setTransactionTracking(long transactionID, long oldestTransactionID) {
    this.transactionID = transactionID;
    this.oldestTransactionID = oldestTransactionID;
  }

  public byte[] asSerializedBytes() {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try {
      DataOutputStream output = new DataOutputStream(bytes);
      output.writeInt(this.type.ordinal());
      output.writeBoolean(this.shouldReplicateToPassives);
      output.writeLong(this.transactionID);
      output.writeLong(this.oldestTransactionID);
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
