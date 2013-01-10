/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.object.serialization;

import org.terracotta.toolkit.object.serialization.NotSerializableRuntimeException;

import java.io.IOException;

public interface SerializationStrategy {

  /**
   * Serialize the given value into byte array.
   * 
   * @param value value to serialize
   * @return serialized form
   * @throws NotSerializableRuntimeException if serialization fails
   */
  public byte[] serialize(Object serializable, boolean compress) throws NotSerializableRuntimeException;

  /**
   * Deserialize the serialized value returning a new representation.
   * 
   * @param data serialized form
   * @return a new deserialized value
   * @throws IOException if deserialization fails
   * @throws ClassNotFoundException if a required class is not found
   */
  public Object deserialize(byte[] fromBytes, boolean compress, boolean local) throws IOException,
      ClassNotFoundException;

  /**
   * Convert the given key into a portable {@code String} form.
   * 
   * @param key key to serialize
   * @return a portable {@code String} key
   * @throws NotSerializableRuntimeException if serialization fails
   */
  public String serializeToString(final Object key) throws NotSerializableRuntimeException;

  /**
   * Deserialize a given key object into the "real" key object. This method assumes it is passed Strings created from
   * serializeToString() on this class
   * 
   * @param key key to deserialize
   * @param localOnly if the deserialization should be attempted only 'locally' without consulting any clustered
   *        operations
   * @return the deserialized key object or null
   * @throws IOException
   * @throws ClassNotFoundException
   */
  public Object deserializeFromString(final String key, boolean localOnly) throws IOException, ClassNotFoundException;

}
