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
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.object.dna.api;

import com.tc.io.TCDataInput;
import com.tc.io.TCDataOutput;

import java.io.IOException;

/**
 * Defines how to encode DNA onto a stream or decode data from a stream, with some different policies for encoding.
 */
public interface DNAEncoding {

  /**
   * Encode an object onto an output stream
   * 
   * @param value The object
   * @param output The output
   */
  public abstract void encode(Object value, TCDataOutput output);

  /**
   * Decode an object from an input stream
   * 
   * @param input The input stream
   */
  public abstract Object decode(TCDataInput input) throws IOException, ClassNotFoundException;

  /**
   * Encode an array onto an output stream, automatically determine array length
   * 
   * @param value The array
   * @param output The output
   */
  public abstract void encodeArray(Object value, TCDataOutput output);

  /**
   * Encode an array onto an output stream
   * 
   * @param value The array
   * @param output The output
   * @param length The length of the array to encode
   */
  public abstract void encodeArray(Object value, TCDataOutput output, int length);

}
