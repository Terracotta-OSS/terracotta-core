/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.bytes;

import com.tc.lang.Recyclable;


/**
 * @author teck TODO: document me!
 */
public interface TCByteBuffer extends Recyclable {

  ////////////////////
  // java.nio.Buffer methods
  ////////////////////
  int capacity();

  TCByteBuffer clear();

  TCByteBuffer flip();

  boolean hasRemaining();

  boolean isReadOnly();

  int limit();

  TCByteBuffer limit(int newLimit);

  // Buffer mark()
  int position();

  TCByteBuffer position(int newPosition);

  int remaining();

  // Buffer reset()
  TCByteBuffer rewind();

  ////////////////////
  // java.nio.ByteBuffer methods
  ////////////////////
  TCByteBuffer asReadOnlyBuffer();

  boolean isDirect();

  byte[] array();

  int arrayOffset();

  byte get();

  TCByteBuffer get(byte[] dst);

  TCByteBuffer get(byte[] dst, int offset, int length);

  byte get(int index);

  boolean getBoolean();

  boolean getBoolean(int index);

  char getChar();

  char getChar(int index);

  double getDouble();

  double getDouble(int index);

  float getFloat();

  float getFloat(int index);

  int getInt();

  int getInt(int index);

  long getLong();

  long getLong(int index);

  short getShort();

  short getShort(int index);

  TCByteBuffer put(byte[] src);

  TCByteBuffer put(byte[] src, int offset, int length);

  TCByteBuffer put(TCByteBuffer src);

  TCByteBuffer put(byte b);

  TCByteBuffer put(int index, byte b);

  TCByteBuffer putBoolean(boolean b);

  TCByteBuffer putBoolean(int index, boolean b);

  TCByteBuffer putChar(char c);

  TCByteBuffer putChar(int index, char c);

  TCByteBuffer putFloat(float f);

  TCByteBuffer putFloat(int index, float f);

  TCByteBuffer putDouble(double d);

  TCByteBuffer putDouble(int index, double d);

  TCByteBuffer putInt(int i);

  TCByteBuffer putInt(int index, int i);

  TCByteBuffer putLong(long l);

  TCByteBuffer putLong(int index, long l);

  TCByteBuffer putShort(short s);

  TCByteBuffer putShort(int index, short s);

  TCByteBuffer duplicate();

  TCByteBuffer slice();

  boolean hasArray();

  ////////////////////
  // TC methods
  ////////////////////

  // access to the underlying JDK14 java.nio.ByteBuffer (if available)
  boolean isNioBuffer();

  Object getNioBuffer(); // return Object on purpose. Client code must cast return value
  
  // absolute bulk get/put methods. It seems like these methods are missing from the java.nio.ByteBuffer interface
  TCByteBuffer get(int index, byte[] dst);

  TCByteBuffer get(int index, byte[] dst, int offset, int length);

  TCByteBuffer put(int index, byte[] src);

  TCByteBuffer put(int index, byte[] src, int offset, int length);

  // write unsigned 4 and 2 byte integer values into byte buffers
  TCByteBuffer putUint(long i);

  TCByteBuffer putUint(int index, long i);

  TCByteBuffer putUshort(int s);

  TCByteBuffer putUshort(int index, int s);

  // get and put unsigned byte values
  short getUbyte();

  short getUbyte(int index);

  TCByteBuffer putUbyte(short value);

  TCByteBuffer putUbyte(int index, short value);

  // get unsigned 4 and 2 byte unsigned integer values from byte buffers
  long getUint();

  long getUint(int index);

  int getUshort();

  int getUshort(int index);

  // XXX: add some zeroOut() methods to write byte 0 into ranges of this buffer
}