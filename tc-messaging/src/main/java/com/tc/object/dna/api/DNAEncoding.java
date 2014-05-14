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
   * When the policy is set to SERIALIZER then the DNAEncoding.decode() will return the exact Objects that where
   * encoded. For Example if UTF8ByteDataHolder is serialized to a stream, then when it is deserialized, you get an
   * UTF8ByteDataHolder object. Same goes for String or ClassHolder etc.
   * <p>
   * You may want such a policy in TCObjectInputStream, for example.
   */
  public static final byte SERIALIZER = 0x00;
  /**
   * When the policy is set to STORAGE then the DNAEncoding.decode() may return Objects that represent the original
   * objects for performance/memory. For Example if String is serialized to a stream, then when it is deserialized, you
   * may get UTF8ByteDataHolder instead.
   * <p>
   * As the name says, you may want such a policy for storage in the L2.
   */
  public static final byte STORAGE    = 0x01;
  /**
   * When the policy is set to APPLICATOR then the DNAEncoding.decode() will return the original Objects that were
   * encoded in the original stream. For Example if UTF8ByteDataHolder is serialized to a stream, then when it is
   * deserialized, you get a String object.
   * <p>
   * You may want such a policy in TCObjectInputStream, for example.
   */
  public static final byte APPLICATOR = 0x02;

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
