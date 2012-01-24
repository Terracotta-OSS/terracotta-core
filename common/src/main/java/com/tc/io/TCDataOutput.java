/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.io;

import java.io.DataOutput;

/**
 * Output stream interface. This interface doesn't declare thrown IOExceptions on writeXXX() methods and adds
 * writeString() for uniform String handling (unlike writeUTF() which has length restrictions)
 */
public interface TCDataOutput extends DataOutput {

  /**
   * Close the stream
   */
  public void close();

  /**
   * Write int
   * 
   * @param value Value
   */
  public void write(int value);

  /**
   * Write byte array
   * 
   * @param value Value
   */
  public void write(byte[] value);

  /**
   * Write byte array from offset of length
   * 
   * @param value Value
   * @param offset Start at offset in value
   * @param length Length to write
   */
  public void write(byte[] value, int offset, int length);

  /**
   * Write boolean
   * 
   * @param value Value
   */
  public void writeBoolean(boolean value);

  /**
   * Write byte
   * 
   * @param value Value
   */
  public void writeByte(int value);

  /**
   * Write char
   * 
   * @param value Value
   */
  public void writeChar(int value);

  /**
   * Write double
   * 
   * @param value Value
   */
  public void writeDouble(double value);

  /**
   * Write float
   * 
   * @param value Value
   */
  public void writeFloat(float value);

  /**
   * Write int
   * 
   * @param value Value
   */
  public void writeInt(int value);

  /**
   * Write long
   * 
   * @param value Value
   */
  public void writeLong(long value);

  /**
   * Write short
   * 
   * @param value Value
   */
  public void writeShort(int value);

  /**
   * Write String
   * 
   * @param value Value
   */
  public void writeString(String string);

}
