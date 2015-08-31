package org.terracotta.passthrough;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;


/**
 * A common utility class to encode/decode passthrough messages since they are stored as byte[] instances, on client and
 * server message queues.
 * Arguably, this is over-kill, as the instances can be passed through, directly (since both the client and server run in
 * the same process).  Serializing them ensures that there are no invalid assumptions being made on either side, however.
 */
public class PassthroughMessageCodec {
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
  }

  public static Type getType(DataInputStream input) throws IOException {
    int ordinal = input.readInt();
    return Type.values()[ordinal];
  }

  public static PassthroughMessage createFetchMessage(Class<?> clazz, String entityName, long clientInstanceID, long version) {
    return new PassthroughMessage() {
      @Override
      protected void populateStream(DataOutputStream output) throws IOException {
        output.writeInt(Type.FETCH_ENTITY.ordinal());
        output.writeUTF(clazz.getCanonicalName());
        output.writeUTF(entityName);
        output.writeLong(clientInstanceID);
        output.writeLong(version);
      }};
  }

  public static PassthroughMessage createReleaseMessage(Class<?> entityClass, String entityName, long clientInstanceID) {
    return new PassthroughMessage() {
      @Override
      protected void populateStream(DataOutputStream output) throws IOException {
        output.writeInt(Type.RELEASE_ENTITY.ordinal());
        output.writeUTF(entityClass.getCanonicalName());
        output.writeUTF(entityName);
        output.writeLong(clientInstanceID);
      }};
  }

  public static PassthroughMessage createExistsMessage(Class<?> entityClass, String entityName, long version) {
    return new PassthroughMessage() {
      @Override
      protected void populateStream(DataOutputStream output) throws IOException {
        output.writeInt(Type.DOES_ENTITY_EXIST.ordinal());
        output.writeUTF(entityClass.getCanonicalName());
        output.writeUTF(entityName);
        output.writeLong(version);
      }};
  }

  public static PassthroughMessage createDestroyMessage(Class<?> entityClass, String entityName) {
    return new PassthroughMessage() {
      @Override
      protected void populateStream(DataOutputStream output) throws IOException {
        output.writeInt(Type.DESTROY_ENTITY.ordinal());
        output.writeUTF(entityClass.getCanonicalName());
        output.writeUTF(entityName);
      }};
  }

  public static PassthroughMessage createCreateMessage(Class<?> entityClass, String entityName, long version, byte[] serializedConfiguration) {
    return new PassthroughMessage() {
      @Override
      protected void populateStream(DataOutputStream output) throws IOException {
        output.writeInt(Type.CREATE_ENTITY.ordinal());
        output.writeUTF(entityClass.getCanonicalName());
        output.writeUTF(entityName);
        output.writeLong(version);
        output.writeInt(serializedConfiguration.length);
        output.write(serializedConfiguration);
      }};
  }

  public static PassthroughMessage createInvokeMessage(Class<?> clazz, String entityName, long clientInstanceID, byte[] payload, boolean shouldReplicateToPassives) {
    return new PassthroughMessage() {
      @Override
      protected void populateStream(DataOutputStream output) throws IOException {
        output.writeInt(Type.INVOKE_ON_SERVER.ordinal());
        output.writeUTF(clazz.getCanonicalName());
        output.writeUTF(entityName);
        output.writeLong(clientInstanceID);
        output.writeInt(payload.length);
        output.write(payload);
      }};
  }

  public static PassthroughMessage createAckMessage() {
    return new PassthroughMessage() {
      @Override
      protected void populateStream(DataOutputStream output) throws IOException {
        output.writeInt(Type.ACK_FROM_SERVER.ordinal());
      }};
  }

  public static PassthroughMessage createCompleteMessage(byte[] response, Exception error) {
    return new PassthroughMessage() {
      @Override
      protected void populateStream(DataOutputStream output) throws IOException {
        output.writeInt(Type.COMPLETE_FROM_SERVER.ordinal());
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

  public static PassthroughMessage createMessageToClient(long clientInstanceID, byte[] payload) {
    return new PassthroughMessage() {
      @Override
      protected void populateStream(DataOutputStream output) throws IOException {
        output.writeInt(Type.INVOKE_ON_CLIENT.ordinal());
        output.writeLong(clientInstanceID);
        output.writeInt(payload.length);
        output.write(payload);
      }};
  }

  public static <R> R decodeRawMessage(Decoder<R> decoder, byte[] rawMessage) {
    return runRawDecoder(decoder, rawMessage);
  }
  
  public static long decodeTransactionIDFromRawMessage(byte[] rawMessage) {
    Decoder<Long> decoder = (DataInputStream input) -> {
      // The transactionID is always the first long in the stream.
      long transactionID = input.readLong();
      return transactionID;
    };
    Long result = runRawDecoder(decoder, rawMessage);
    return result.longValue();
  }
  
  public static Type decodeTransactionTypeFromRawMessage(byte[] rawMessage) {
    Decoder<Type> decoder = (DataInputStream input) -> {
      // The type is an int ordinal after the transactionID.
      input.readLong();
      int ordinal = input.readInt();
      return Type.values()[ordinal];
    };
    return runRawDecoder(decoder, rawMessage);
  }
  
  public static byte[] serializeObjectToArray(Exception exception) {
    // We need to manually serialize the exception using Java serialization.
    ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
    try (ObjectOutputStream objectOutput = new ObjectOutputStream(byteOutput)) {
      objectOutput.writeObject(exception);
    } catch (IOException e) {
      // Can't happen with a byte array.
      Assert.unexpected(e);
    }
    return byteOutput.toByteArray();
  }
  
  public static Exception deserializeExceptionFromArray(byte[] bytes) {
    Exception exception = null;
    ByteArrayInputStream byteInput = new ByteArrayInputStream(bytes);
    try (ObjectInputStream objectInput = new ObjectInputStream(byteInput)) {
      exception = (Exception) objectInput.readObject();
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
      result = decoder.decode(input);
    } catch (IOException e) {
      // Can't happen with a byte array.
      Assert.unexpected(e);
    }
    return result;
  }

  public interface Decoder<R> {
    public R decode(DataInputStream input) throws IOException;
  }
}
