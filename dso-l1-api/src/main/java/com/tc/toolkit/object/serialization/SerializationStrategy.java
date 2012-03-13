/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.toolkit.object.serialization;

import java.io.IOException;
import java.io.Serializable;

public interface SerializationStrategy<T extends Serializable> {

  /**
   * Serialize the given value into byte array.
   * 
   * @param value value to serialize
   * @return serialized form
   * @throws IOException if serialization fails
   */
  public byte[] serialize(T serializable) throws IOException;

  /**
   * Deserialize the serialized value returning a new representation.
   * 
   * @param data serialized form
   * @return a new deserialized value
   * @throws IOException if deserialization fails
   * @throws ClassNotFoundException if a required class is not found
   */
  public T deserialize(byte[] fromBytes) throws IOException, ClassNotFoundException;

  /**
   * Convert the given key into a portable {@code String} form.
   * 
   * @param key key to serialize
   * @return a portable {@code String} key
   * @throws IOException if serialization fails
   */
  public String serializeToString(final Serializable key) throws IOException;

  /**
   * Deserialize a given key object into the "real" key object. This method assumes it is passed Strings created from
   * generateStringKeyFor() on this class
   * 
   * @param key key to deserialize
   * @return the deserialized key object
   * @throws IOException
   * @throws ClassNotFoundException
   */
  public Serializable deserializeFromString(final String key) throws IOException, ClassNotFoundException;

}
