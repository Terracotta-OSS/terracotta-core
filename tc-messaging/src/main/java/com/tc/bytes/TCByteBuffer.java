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
package com.tc.bytes;

import java.nio.ByteBuffer;

public interface TCByteBuffer {

  public TCByteBuffer clear();

  public TCByteBuffer reInit();

  public int capacity();

  public int position();

  public TCByteBuffer flip();

  public boolean hasRemaining();

  public int limit();

  public TCByteBuffer limit(int newLimit);

  public TCByteBuffer position(int newPosition);

  public int remaining();

  public com.tc.bytes.TCByteBuffer rewind();

  public ByteBuffer getNioBuffer();

  public boolean isDirect();

  public byte[] array();

  public byte get();

  public boolean getBoolean();

  public boolean getBoolean(int index);

  public char getChar();

  public char getChar(int index);

  public double getDouble();

  public double getDouble(int index);

  public float getFloat();

  public float getFloat(int index);

  public int getInt();

  public int getInt(int index);

  public long getLong();

  public long getLong(int index);

  public short getShort();

  public short getShort(int index);

  public TCByteBuffer get(byte[] dst);

  public TCByteBuffer get(byte[] dst, int offset, int length);

  public byte get(int index);

  public TCByteBuffer put(byte b);

  public TCByteBuffer put(byte[] src);

  public TCByteBuffer put(byte[] src, int offset, int length);

  public TCByteBuffer put(int index, byte b);

  public TCByteBuffer putBoolean(boolean b);

  public TCByteBuffer putBoolean(int index, boolean b);

  public TCByteBuffer putChar(char c);

  public TCByteBuffer putChar(int index, char c);

  public TCByteBuffer putDouble(double d);

  public TCByteBuffer putDouble(int index, double d);

  public TCByteBuffer putFloat(float f);

  public TCByteBuffer putFloat(int index, float f);

  public TCByteBuffer putInt(int i);

  public TCByteBuffer putInt(int index, int i);

  public TCByteBuffer putLong(long l);

  public TCByteBuffer putLong(int index, long l);

  public TCByteBuffer putShort(short s);

  public TCByteBuffer putShort(int index, short s);

  public TCByteBuffer duplicate();

  public TCByteBuffer put(TCByteBuffer src);

  public TCByteBuffer slice();

  public int arrayOffset();

  public TCByteBuffer asReadOnlyBuffer();

  public boolean isReadOnly();

  public boolean hasArray();

  public TCByteBuffer get(int index, byte[] dst);

  public TCByteBuffer get(int index, byte[] dst, int offset, int length);

  public TCByteBuffer put(int index, byte[] src);

  public TCByteBuffer put(int index, byte[] src, int offset, int length);

  public TCByteBuffer putUint(long i);

  public TCByteBuffer putUint(int index, long i);

  public TCByteBuffer putUshort(int s);

  public TCByteBuffer putUshort(int index, int s);

  public long getUint();

  public long getUint(int index);

  public int getUshort();

  public int getUshort(int index);

  public short getUbyte();

  public short getUbyte(int index);

  public TCByteBuffer putUbyte(int index, short value);

  public TCByteBuffer putUbyte(short value);

}