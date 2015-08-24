package org.terracotta.passthrough;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.terracotta.passthrough.PassthroughMessage.Type;


/**
 * A common utility class to encode/decode passthrough messages since they are stored as byte[] instances, on client and
 * server message queues.
 * Arguably, this is over-kill, as the instances can be passed through, directly (since both the client and server run in
 * the same process).  Serializing them ensures that there are no invalid assumptions being made on either side, however.
 */
public class PassthroughMessageCodec {
  public static PassthroughMessage createFetchMessage(final Class<?> clazz, final String entityName, final long clientInstanceID, final long version) {
    boolean shouldReplicateToPassives = false;
    return new PassthroughMessage(Type.FETCH_ENTITY, shouldReplicateToPassives) {
      @Override
      protected void populateStream(DataOutputStream output) throws IOException {
        output.writeUTF(clazz.getCanonicalName());
        output.writeUTF(entityName);
        output.writeLong(clientInstanceID);
        output.writeLong(version);
      }};
  }

  public static PassthroughMessage createReleaseMessage(final Class<?> entityClass, final String entityName, final long clientInstanceID) {
    boolean shouldReplicateToPassives = false;
    return new PassthroughMessage(Type.RELEASE_ENTITY, shouldReplicateToPassives) {
      @Override
      protected void populateStream(DataOutputStream output) throws IOException {
        output.writeUTF(entityClass.getCanonicalName());
        output.writeUTF(entityName);
        output.writeLong(clientInstanceID);
      }};
  }

  public static PassthroughMessage createExistsMessage(final Class<?> entityClass, final String entityName, final long version) {
    boolean shouldReplicateToPassives = false;
    return new PassthroughMessage(Type.DOES_ENTITY_EXIST, shouldReplicateToPassives) {
      @Override
      protected void populateStream(DataOutputStream output) throws IOException {
        output.writeUTF(entityClass.getCanonicalName());
        output.writeUTF(entityName);
        output.writeLong(version);
      }};
  }

  public static PassthroughMessage createDestroyMessage(final Class<?> entityClass, final String entityName) {
    boolean shouldReplicateToPassives = true;
    return new PassthroughMessage(Type.DESTROY_ENTITY, shouldReplicateToPassives) {
      @Override
      protected void populateStream(DataOutputStream output) throws IOException {
        output.writeUTF(entityClass.getCanonicalName());
        output.writeUTF(entityName);
      }};
  }

  public static PassthroughMessage createCreateMessage(final Class<?> entityClass, final String entityName, final long version, final byte[] serializedConfiguration) {
    boolean shouldReplicateToPassives = true;
    return new PassthroughMessage(Type.CREATE_ENTITY, shouldReplicateToPassives) {
      @Override
      protected void populateStream(DataOutputStream output) throws IOException {
        output.writeUTF(entityClass.getCanonicalName());
        output.writeUTF(entityName);
        output.writeLong(version);
        output.writeInt(serializedConfiguration.length);
        output.write(serializedConfiguration);
      }};
  }

  public static PassthroughMessage createInvokeMessage(final Class<?> clazz, final String entityName, final long clientInstanceID, final byte[] payload, final boolean shouldReplicateToPassives) {
    return new PassthroughMessage(Type.INVOKE_ON_SERVER, shouldReplicateToPassives) {
      @Override
      protected void populateStream(DataOutputStream output) throws IOException {
        output.writeUTF(clazz.getCanonicalName());
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

  public static PassthroughMessage createCompleteMessage(final byte[] response, final Exception error) {
    // Replication ignored in this context.
    boolean shouldReplicateToPassives = false;
    return new PassthroughMessage(Type.COMPLETE_FROM_SERVER, shouldReplicateToPassives) {
      @Override
      protected void populateStream(DataOutputStream output) throws IOException {
        boolean isSuccess = (null == error);
        output.writeBoolean(isSuccess);
        if (isSuccess) {
          if (null != response) {
            output.writeInt(response.length);
            output.write(response);
          } else {
            output.writeInt(-1);
          }
        } else {
          byte[] serializedException = PassthroughMessageCodec.serializeObjectToArray(error);
          output.writeInt(serializedException.length);
          output.write(serializedException);
        }
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

  public static <R> R decodeRawMessage(Decoder<R> decoder, byte[] rawMessage) {
    return runRawDecoder(decoder, rawMessage);
  }
  
  public static long decodeTransactionIDFromRawMessage(byte[] rawMessage) {
    Decoder<Long> decoder = new Decoder<Long>() {

      @Override
      public Long decode(Type type, boolean shouldReplicate, long transactionID, DataInputStream input) throws IOException {
        return transactionID;
      }
    };
    return runRawDecoder(decoder, rawMessage);
  }
  
  public static Type decodeTransactionTypeFromRawMessage(byte[] rawMessage) {
    Decoder<Type> decoder = new Decoder<Type>() {

      @Override
      public Type decode(Type type, boolean shouldReplicate, long transactionID, DataInputStream input) throws IOException {
        // The type is an int ordinal after the transactionID.
        input.readLong();
        int ordinal = input.readInt();
        return Type.values()[ordinal];
      }
    };
    return runRawDecoder(decoder, rawMessage);
  }
  
  public static byte[] serializeObjectToArray(Exception exception) {
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
  
  public static Exception deserializeExceptionFromArray(byte[] bytes) {
    Exception exception = null;
    ByteArrayInputStream byteInput = new ByteArrayInputStream(bytes);
    try {
      ObjectInputStream objectInput = new ObjectInputStream(byteInput);
      try {
        exception = (Exception) objectInput.readObject();
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
      result = decoder.decode(type, shouldReplicate, transactionID, input);
    } catch (IOException e) {
      // Can't happen with a byte array.
      Assert.unexpected(e);
    }
    return result;
  }

  public interface Decoder<R> {
    public R decode(Type type, boolean shouldReplicate, long transactionID, DataInputStream input) throws IOException;
  }
}
