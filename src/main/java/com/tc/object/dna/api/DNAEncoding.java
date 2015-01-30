/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
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
