/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
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
  @Override
  public void write(int value);

  /**
   * Write byte array
   * 
   * @param value Value
   */
  @Override
  public void write(byte[] value);

  /**
   * Write byte array from offset of length
   * 
   * @param value Value
   * @param offset Start at offset in value
   * @param length Length to write
   */
  @Override
  public void write(byte[] value, int offset, int length);

  /**
   * Write boolean
   * 
   * @param value Value
   */
  @Override
  public void writeBoolean(boolean value);

  /**
   * Write byte
   * 
   * @param value Value
   */
  @Override
  public void writeByte(int value);

  /**
   * Write char
   * 
   * @param value Value
   */
  @Override
  public void writeChar(int value);

  /**
   * Write double
   * 
   * @param value Value
   */
  @Override
  public void writeDouble(double value);

  /**
   * Write float
   * 
   * @param value Value
   */
  @Override
  public void writeFloat(float value);

  /**
   * Write int
   * 
   * @param value Value
   */
  @Override
  public void writeInt(int value);

  /**
   * Write long
   * 
   * @param value Value
   */
  @Override
  public void writeLong(long value);

  /**
   * Write short
   * 
   * @param value Value
   */
  @Override
  public void writeShort(int value);

  /**
   * Write String
   * 
   * @param value Value
   */
  public void writeString(String string);

}
