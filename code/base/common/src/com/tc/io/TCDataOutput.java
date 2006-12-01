/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.io;


public interface TCDataOutput {

  public void close();

  public void write(int value);

  public void write(byte[] value);

  public void write(byte[] value, int offset, int length);

  public void writeBoolean(boolean value);

  public void writeByte(int value);

  public void writeChar(int value);

  public void writeDouble(double value);

  public void writeFloat(float value);

  public void writeInt(int value);

  public void writeLong(long value);

  public void writeShort(int value);
  
  public void writeString(String string);

}
