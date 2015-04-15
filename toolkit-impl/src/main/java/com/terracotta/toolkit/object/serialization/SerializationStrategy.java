/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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
