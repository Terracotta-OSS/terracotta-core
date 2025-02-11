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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.terracotta.exception.EntityException;
import org.terracotta.passthrough.PassthroughMessage.Type;


/**
 * A common utility class to encode/decode passthrough messages since they are stored as byte[] instances, on client and
 * server message queues.
 * Arguably, this is over-kill, as the instances can be passed through, directly (since both the client and server run in
 * the same process).  Serializing them ensures that there are no invalid assumptions being made on either side, however.
 */
public class PassthroughMessageCodec {
  public static PassthroughMessage createFetchMessage(final String entityClassName, final String entityName, final long clientInstanceID, final long version) {
    boolean shouldReplicateToPassives = false;
    return new PassthroughMessage(Type.FETCH_ENTITY, shouldReplicateToPassives) {
      @Override
      protected void populateStream(DataOutputStream output) throws IOException {
        output.writeUTF(entityClassName);
        output.writeUTF(entityName);
        output.writeLong(clientInstanceID);
        output.writeLong(version);
      }};
  }

  public static PassthroughMessage createReleaseMessage(final String entityClassName, final String entityName, final long clientInstanceID) {
    boolean shouldReplicateToPassives = true;
    return new PassthroughMessage(Type.RELEASE_ENTITY, shouldReplicateToPassives) {
      @Override
      protected void populateStream(DataOutputStream output) throws IOException {
        output.writeUTF(entityClassName);
        output.writeUTF(entityName);
        output.writeLong(clientInstanceID);
      }};
  }

  public static PassthroughMessage createUnexpectedReleaseMessage(final String entityClassName, final String entityName, final long clientInstanceID) {
    boolean shouldReplicateToPassives = true;
    return new PassthroughMessage(Type.UNEXPECTED_RELEASE, shouldReplicateToPassives) {
      @Override
      protected void populateStream(DataOutputStream output) throws IOException {
        output.writeUTF(entityClassName);
        output.writeUTF(entityName);
        output.writeLong(clientInstanceID);
      }};
  }

  public static PassthroughMessage createExistsMessage(final String entityClassName, final String entityName, final long version) {
    boolean shouldReplicateToPassives = false;
    return new PassthroughMessage(Type.DOES_ENTITY_EXIST, shouldReplicateToPassives) {
      @Override
      protected void populateStream(DataOutputStream output) throws IOException {
        output.writeUTF(entityClassName);
        output.writeUTF(entityName);
        output.writeLong(version);
      }};
  }

  public static PassthroughMessage createDestroyMessage(final String entityClassName, final String entityName) {
    boolean shouldReplicateToPassives = true;
    return new PassthroughMessage(Type.DESTROY_ENTITY, shouldReplicateToPassives) {
      @Override
      protected void populateStream(DataOutputStream output) throws IOException {
        output.writeUTF(entityClassName);
        output.writeUTF(entityName);
      }};
  }

  public static PassthroughMessage createCreateMessage(final String entityClassName, final String entityName, final long version, final byte[] serializedConfiguration) {
    boolean shouldReplicateToPassives = true;
    return new PassthroughMessage(Type.CREATE_ENTITY, shouldReplicateToPassives) {
      @Override
      protected void populateStream(DataOutputStream output) throws IOException {
        output.writeUTF(entityClassName);
        output.writeUTF(entityName);
        output.writeLong(version);
        output.writeInt(serializedConfiguration.length);
        output.write(serializedConfiguration);
      }};
  }
  
  public static PassthroughMessage createReconfigureMessage(final String entityClassName, final String entityName, final long version, final byte[] serializedConfiguration) {
    boolean shouldReplicateToPassives = true;
    return new PassthroughMessage(Type.RECONFIGURE_ENTITY, shouldReplicateToPassives) {
      @Override
      protected void populateStream(DataOutputStream output) throws IOException {
        output.writeUTF(entityClassName);
        output.writeUTF(entityName);
        output.writeLong(version);
        output.writeInt(serializedConfiguration.length);
        output.write(serializedConfiguration);
      }};
  }
  
  public static PassthroughMessage createInvokeMessage(final String entityClassName, final String entityName, final long clientInstanceID, final byte[] payload, final boolean shouldReplicateToPassives) {
    return new PassthroughMessage(Type.INVOKE_ON_SERVER, shouldReplicateToPassives) {
      @Override
      protected void populateStream(DataOutputStream output) throws IOException {
        output.writeUTF(entityClassName);
        output.writeUTF(entityName);
        output.writeLong(clientInstanceID);
        output.writeInt(payload.length);
        output.write(payload);
      }};
  }

  public static PassthroughMessage createAckMessage() {
    // Replication ignored in this context.
    boolean shouldReplicateToPassives = false;
    return new PassthroughMessage(Type.ACK_FROM_SERVER, shouldReplicateToPassives) {
      @Override
      protected void populateStream(DataOutputStream output) throws IOException {
        output.writeInt(Type.ACK_FROM_SERVER.ordinal());
      }};
  }

  public static PassthroughMessage createMonitorMessage(final byte[] response, final EntityException error) {
    // Replication ignored in this context.
    boolean shouldReplicateToPassives = false;
    boolean isSuccess = (null == error);
    return new PassthroughMessage(isSuccess ? Type.MONITOR_MESSAGE : Type.MONITOR_EXCEPTION, shouldReplicateToPassives) {
      @Override
      protected void populateStream(DataOutputStream output) throws IOException {
        if (isSuccess) {
          if (null != response) {
            output.writeInt(response.length);
            output.write(response);
          } else {
            output.writeInt(-1);
          }
        } else {
          byte[] serializedException = PassthroughMessageCodec.serializeExceptionToArray(error);
          output.writeInt(serializedException.length);
          output.write(serializedException);
        }
      }};
  }

  public static PassthroughMessage createCompleteMessage(final byte[] response, final EntityException error) {
    // Replication ignored in this context.
    boolean shouldReplicateToPassives = false;
    boolean isSuccess = (null == error);
    return new PassthroughMessage(isSuccess ? Type.COMPLETE_FROM_SERVER : Type.EXCEPTION_FROM_SERVER, shouldReplicateToPassives) {
      @Override
      protected void populateStream(DataOutputStream output) throws IOException {
        if (isSuccess) {
          if (null != response) {
            output.writeInt(response.length);
            output.write(response);
          } else {
            output.writeInt(-1);
          }
        } else {
          byte[] serializedException = PassthroughMessageCodec.serializeExceptionToArray(error);
          output.writeInt(serializedException.length);
          output.write(serializedException);
        }
      }};
  }

  public static PassthroughMessage createRetireMessage() {
    // Replication ignored in this context.
    boolean shouldReplicateToPassives = false;
    return new PassthroughMessage(Type.RETIRE_FROM_SERVER, shouldReplicateToPassives) {
      @Override
      protected void populateStream(DataOutputStream output) throws IOException {
        output.writeInt(Type.RETIRE_FROM_SERVER.ordinal());
      }};
  }

  public static PassthroughMessage createMessageToClient(final long clientInstanceID, final byte[] payload) {
    // Replication ignored in this context.
    boolean shouldReplicateToPassives = false;
    return new PassthroughMessage(Type.INVOKE_ON_CLIENT, shouldReplicateToPassives) {
      @Override
      protected void populateStream(DataOutputStream output) throws IOException {
        output.writeLong(clientInstanceID);
        output.writeInt(payload.length);
        output.write(payload);
      }};
  }

  public static PassthroughMessage createReconnectMessage(final String entityClassName, final String entityName, final long clientInstanceID, final byte[] extendedData) {
    Assert.assertTrue(null != extendedData);
    // This is equivalent to a FETCH so we don't care about replication.
    boolean shouldReplicateToPassives = false;
    return new PassthroughMessage(Type.RECONNECT, shouldReplicateToPassives) {
      @Override
      protected void populateStream(DataOutputStream output) throws IOException {
        output.writeUTF(entityClassName);
        output.writeUTF(entityName);
        output.writeLong(clientInstanceID);
        output.writeInt(extendedData.length);
        output.write(extendedData);
      }};
  }

  public static PassthroughMessage createSyncEntityStartMessage(final String entityClassName, final String entityName, final long version, final byte[] serializedConfiguration) {
    // Replication is meaningless for sync.
    boolean shouldReplicateToPassives = false;
    return new PassthroughMessage(Type.SYNC_ENTITY_START, shouldReplicateToPassives) {
      @Override
      protected void populateStream(DataOutputStream output) throws IOException {
        output.writeUTF(entityClassName);
        output.writeUTF(entityName);
        output.writeLong(version);
        output.writeInt(serializedConfiguration.length);
        output.write(serializedConfiguration);
      }};
  }

  public static PassthroughMessage createSyncEntityEndMessage(final String entityClassName, final String entityName) {
    // Replication is meaningless for sync.
    boolean shouldReplicateToPassives = false;
    return new PassthroughMessage(Type.SYNC_ENTITY_END, shouldReplicateToPassives) {
      @Override
      protected void populateStream(DataOutputStream output) throws IOException {
        output.writeUTF(entityClassName);
        output.writeUTF(entityName);
      }};
  }

  public static PassthroughMessage createSyncEntityKeyStartMessage(final String entityClassName, final String entityName, final int concurrencyKey) {
    // Replication is meaningless for sync.
    boolean shouldReplicateToPassives = false;
    return new PassthroughMessage(Type.SYNC_ENTITY_KEY_START, shouldReplicateToPassives) {
      @Override
      protected void populateStream(DataOutputStream output) throws IOException {
        output.writeUTF(entityClassName);
        output.writeUTF(entityName);
        output.writeInt(concurrencyKey);
      }};
  }

  public static PassthroughMessage createSyncEntityKeyEndMessage(final String entityClassName, final String entityName, final int concurrencyKey) {
    // Replication is meaningless for sync.
    boolean shouldReplicateToPassives = false;
    return new PassthroughMessage(Type.SYNC_ENTITY_KEY_END, shouldReplicateToPassives) {
      @Override
      protected void populateStream(DataOutputStream output) throws IOException {
        output.writeUTF(entityClassName);
        output.writeUTF(entityName);
        output.writeInt(concurrencyKey);
      }};
  }

  public static PassthroughMessage createSyncPayloadMessage(final String entityClassName, final String entityName, final int concurrencyKey, final byte[] payload) {
    // Replication is meaningless for sync.
    boolean shouldReplicateToPassives = false;
    return new PassthroughMessage(Type.SYNC_ENTITY_PAYLOAD, shouldReplicateToPassives) {
      @Override
      protected void populateStream(DataOutputStream output) throws IOException {
        output.writeUTF(entityClassName);
        output.writeUTF(entityName);
        output.writeInt(concurrencyKey);
        output.writeInt(payload.length);
        output.write(payload);
      }};
  }

  public static <R> R decodeRawMessage(Decoder<R> decoder, byte[] rawMessage) {
    return runRawDecoder(decoder, rawMessage);
  }
  
  public static long decodeTransactionIDFromRawMessage(byte[] rawMessage) {
    Decoder<Long> decoder = new Decoder<Long>() {

      @Override
      public Long decode(Type type, boolean shouldReplicate, long transactionID, long oldestTransactionID, DataInputStream input) throws IOException {
        return transactionID;
      }
    };
    return runRawDecoder(decoder, rawMessage);
  }
  
  public static Type decodeTransactionTypeFromRawMessage(byte[] rawMessage) {
    Decoder<Type> decoder = new Decoder<Type>() {

      @Override
      public Type decode(Type type, boolean shouldReplicate, long transactionID, long oldestTransactionID, DataInputStream input) throws IOException {
        // The type is an int ordinal after the transactionID.
        input.readLong();
        int ordinal = input.readInt();
        return Type.values()[ordinal];
      }
    };
    return runRawDecoder(decoder, rawMessage);
  }
  
  public static byte[] serializeExceptionToArray(EntityException exception) {
    // We need to manually serialize the exception using Java serialization.
    ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
    try {
      ObjectOutputStream objectOutput = new ObjectOutputStream(byteOutput);
      try {
        objectOutput.writeObject(exception);
      } finally {
        objectOutput.close();
      }
    } catch (IOException e) {
      // Can't happen with a byte array.
      Assert.unexpected(e);
    }
    return byteOutput.toByteArray();
  }
  
  public static EntityException deserializeExceptionFromArray(byte[] bytes) {
    EntityException exception = null;
    ByteArrayInputStream byteInput = new ByteArrayInputStream(bytes);
    try {
      ObjectInputStream objectInput = new ObjectInputStream(byteInput);
      try {
        Object object = objectInput.readObject();
        exception = (EntityException) object;
      } finally {
        objectInput.close();
      }
    } catch (ClassNotFoundException e) {
      // We control this entire system so we should never fail to find the class.
      Assert.unexpected(e);
    } catch (IOException e) {
      // Can't happen with a byte array.
      Assert.unexpected(e);
    }
    return exception;
  }



  private static <R> R runRawDecoder(Decoder<R> decoder, byte[] rawMessage) {
    DataInputStream input = new DataInputStream(new ByteArrayInputStream(rawMessage));
    R result = null;
    try {
      int ordinal = input.readInt();
      Type type = Type.values()[ordinal];
      boolean shouldReplicate = input.readBoolean();
      long transactionID = input.readLong();
      long oldestTransactionID = input.readLong();
      result = decoder.decode(type, shouldReplicate, transactionID, oldestTransactionID, input);
    } catch (IOException e) {
      // Can't happen with a byte array.
      Assert.unexpected(e);
    }
    return result;
  }

  public interface Decoder<R> {
    public R decode(Type type, boolean shouldReplicate, long transactionID, long oldestTransactionID, DataInputStream input) throws IOException;
  }
}
