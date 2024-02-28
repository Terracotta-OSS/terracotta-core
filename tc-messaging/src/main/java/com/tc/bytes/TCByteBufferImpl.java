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

import com.tc.util.Assert;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author teck A thin wrapper to a real java.nio.ByteBuffer instance
 */

// XXX: Should we wrap the native java.nio overflow, underflow and readOnly exceptions with the TC versions?
// This would make the TCByteBuffer interface consistent w.r.t. exceptions (whilst being blind to JDK13 vs JDK14)
public class TCByteBufferImpl implements TCByteBuffer {
  private final TCByteBuffer        source;
  private final ByteBuffer          hiddenBuffer;
  private static final boolean ACCESS_CHECK = Boolean.getBoolean("buffer.access.check");
  private final AtomicBoolean lock =  ACCESS_CHECK ? new AtomicBoolean() : null;
  private final AtomicInteger references = ACCESS_CHECK ? new AtomicInteger() : null;

  TCByteBufferImpl(int capacity, boolean direct) {
    this(null, direct ? ByteBuffer.allocateDirect(capacity) : ByteBuffer.allocate(capacity));
  }

  private TCByteBufferImpl(TCByteBuffer src, ByteBuffer buf) {
    source = src;
    hiddenBuffer = buf;
  }

  static TCByteBuffer wrap(byte[] data) {
    return new TCByteBufferImpl(null, ByteBuffer.wrap(data));
  }

  protected ByteBuffer getBuffer() {
    return accessBuffer();
  }

  @Override
  public TCByteBuffer clear() {
    accessBuffer().clear();
    return this;
  }

  @Override
  public int capacity() {
    return accessBuffer().capacity();
  }

  @Override
  public int position() {
    return accessBuffer().position();
  }

  @Override
  public TCByteBuffer flip() {
    accessBuffer().flip();
    return this;
  }

  @Override
  public TCByteBuffer compact() {
    accessBuffer().compact();
    return this;
  }

  @Override
  public boolean hasRemaining() {
    return accessBuffer().hasRemaining();
  }

  @Override
  public int limit() {
    return accessBuffer().limit();
  }

  @Override
  public TCByteBuffer limit(int newLimit) {
    accessBuffer().limit(newLimit);
    return this;
  }

  @Override
  public TCByteBuffer position(int newPosition) {
    accessBuffer().position(newPosition);
    return this;
  }

  @Override
  public int remaining() {
    return accessBuffer().remaining();
  }

  @Override
  public com.tc.bytes.TCByteBuffer rewind() {
    accessBuffer().rewind();
    return this;
  }
  
  private void incrementBufferReference() {
    if (references != null) {
      references.incrementAndGet();
    }
  }
  
  private void decrementBufferReference() {
    if (references != null) {
      references.decrementAndGet();
    }
  }
  
  @Override
  public ByteBuffer getNioBuffer() {
    ByteBuffer buffer = accessBuffer();
    incrementBufferReference();
    return buffer;
  }

  @Override
  public void returnNioBuffer(ByteBuffer buffer) {
    if (buffer == accessBuffer()) {
      decrementBufferReference();
    } else {
      throw new IllegalArgumentException("buffer is not owned");
    }
  }

  @Override
  public boolean isDirect() {
    return accessBuffer().isDirect();
  }

  @Override
  public byte[] array() {
    return accessBuffer().array();
  }

  @Override
  public byte get() {
    return accessBuffer().get();
  }

  @Override
  public boolean getBoolean() {
    // XXX: Um-- why isn't there a getBoolean in ByteBuffer?
    return accessBuffer().get() > 0;
  }

  @Override
  public boolean getBoolean(int index) {
    return accessBuffer().get(index) > 0;
  }

  @Override
  public char getChar() {
    return accessBuffer().getChar();
  }

  @Override
  public char getChar(int index) {
    return accessBuffer().getChar(index);
  }

  @Override
  public double getDouble() {
    return accessBuffer().getDouble();
  }

  @Override
  public double getDouble(int index) {
    return accessBuffer().getDouble(index);
  }

  @Override
  public float getFloat() {
    return accessBuffer().getFloat();
  }

  @Override
  public float getFloat(int index) {
    return accessBuffer().getFloat(index);
  }

  @Override
  public int getInt() {
    return accessBuffer().getInt();
  }

  @Override
  public int getInt(int index) {
    return accessBuffer().getInt(index);
  }

  @Override
  public long getLong() {
    return accessBuffer().getLong();
  }

  @Override
  public long getLong(int index) {
    return accessBuffer().getLong(index);
  }

  @Override
  public short getShort() {
    return accessBuffer().getShort();
  }

  @Override
  public short getShort(int index) {
    return accessBuffer().getShort(index);
  }

  @Override
  public TCByteBuffer get(byte[] dst) {
    accessBuffer().get(dst);
    return this;
  }

  @Override
  public TCByteBuffer get(byte[] dst, int offset, int length) {
    accessBuffer().get(dst, offset, length);
    return this;
  }

  @Override
  public byte get(int index) {
    return accessBuffer().get(index);
  }

  @Override
  public TCByteBuffer put(byte b) {
    accessBuffer().put(b);
    return this;
  }

  @Override
  public TCByteBuffer put(byte[] src) {
    accessBuffer().put(src);
    return this;
  }

  @Override
  public TCByteBuffer put(byte[] src, int offset, int length) {
    accessBuffer().put(src, offset, length);
    return this;
  }

  @Override
  public TCByteBuffer put(int index, byte b) {
    accessBuffer().put(index, b);
    return this;
  }

  @Override
  public TCByteBuffer putBoolean(boolean b) {
    // XXX: Why isn't there a putBoolean in ByteBuffer?
    accessBuffer().put((b) ? (byte) 1 : (byte) 0);
    return this;
  }

  @Override
  public TCByteBuffer putBoolean(int index, boolean b) {
    accessBuffer().put(index, (b) ? (byte) 1 : (byte) 0);
    return this;
  }

  @Override
  public TCByteBuffer putChar(char c) {
    accessBuffer().putChar(c);
    return this;
  }

  @Override
  public TCByteBuffer putChar(int index, char c) {
    accessBuffer().putChar(index, c);
    return this;
  }

  @Override
  public TCByteBuffer putDouble(double d) {
    accessBuffer().putDouble(d);
    return this;
  }

  @Override
  public TCByteBuffer putDouble(int index, double d) {
    accessBuffer().putDouble(index, d);
    return this;
  }

  @Override
  public TCByteBuffer putFloat(float f) {
    accessBuffer().putFloat(f);
    return this;
  }

  @Override
  public TCByteBuffer putFloat(int index, float f) {
    accessBuffer().putFloat(index, f);
    return this;
  }

  @Override
  public TCByteBuffer putInt(int i) {
    accessBuffer().putInt(i);
    return this;
  }

  @Override
  public TCByteBuffer putInt(int index, int i) {
    accessBuffer().putInt(index, i);
    return this;
  }

  @Override
  public TCByteBuffer putLong(long l) {
    accessBuffer().putLong(l);
    return this;
  }

  @Override
  public TCByteBuffer putLong(int index, long l) {
    accessBuffer().putLong(index, l);
    return this;
  }

  @Override
  public TCByteBuffer putShort(short s) {
    accessBuffer().putShort(s);
    return this;
  }

  @Override
  public TCByteBuffer putShort(int index, short s) {
    accessBuffer().putShort(index, s);
    return this;
  }

  @Override
  public TCByteBuffer duplicate() {
    return new TCByteBufferImpl(this, accessBuffer().duplicate());
  }

  @Override
  public TCByteBuffer put(TCByteBuffer src) {
    accessBuffer().put(src.getNioBuffer());
    return this;
  }

  @Override
  public TCByteBuffer slice() {
    return new TCByteBufferImpl(this, accessBuffer().slice());
  }

  @Override
  public int arrayOffset() {
    return accessBuffer().arrayOffset();
  }

  @Override
  public TCByteBuffer asReadOnlyBuffer() {
    return new TCByteBufferImpl(this, accessBuffer().asReadOnlyBuffer());
  }

  @Override
  public boolean isReadOnly() {
    return accessBuffer().isReadOnly();
  }

  @Override
  public String toString() {
    return (accessBuffer() == null) ? "TCByteBufferJDK14(null buffer)" : "TCByteBuffer@" + System.identityHashCode(this)
                                                                 + "(" + accessBuffer().toString() + ")";
  }

  @Override
  public boolean hasArray() {
    return accessBuffer().hasArray();
  }
  
  private void checkReferences() {
    if (references != null && references.get() > 0) {
      throw new IllegalStateException("Nio buffer still referenced " + references.get());
    }
  }

  @Override
  public TCByteBuffer reInit() {
    checkReferences();
    clear();
    lock();
    return this;
  }

  @Override
  public final TCByteBuffer get(int index, byte[] dst) {
    return get(index, dst, 0, dst.length);
  }

  @Override
  public final TCByteBuffer get(int index, byte[] dst, int offset, int length) {
    final int origPosition = position();

    try {
      position(index);
      get(dst, offset, length);
    } finally {
      position(origPosition);
    }

    return this;
  }

  @Override
  public final TCByteBuffer put(int index, byte[] src) {
    return put(index, src, 0, src.length);
  }

  @Override
  public final TCByteBuffer put(int index, byte[] src, int offset, int length) {
    final int origPosition = position();

    try {
      position(index);
      put(src, offset, length);
    } finally {
      position(origPosition);
    }

    return this;
  }

  @Override
  public final TCByteBuffer putUint(long i) {
    if ((i > 0xFFFFFFFFL) || (i < 0L)) {
      // make code formatter sane
      throw new IllegalArgumentException("Unsigned integer value must be positive and <= (2^32)-1");
    }

    put((byte) ((i >> 24) & 0x000000FF));
    put((byte) ((i >> 16) & 0x000000FF));
    put((byte) ((i >> 8) & 0x000000FF));
    put((byte) (i & 0x000000FF));

    return this;
  }

  @Override
  public final TCByteBuffer putUint(int index, long i) {
    final int origPosition = position();

    try {
      position(index);
      putUint(i);
    } finally {
      position(origPosition);
    }

    return this;
  }

  @Override
  public final TCByteBuffer putUshort(int s) {
    if ((s > 0x0000FFFF) || (s < 0)) { throw new IllegalArgumentException(
                                                                          "Unsigned integer value must be positive and <= (2^16)-1"); }

    put((byte) ((s >> 8) & 0x000000FF));
    put((byte) (s & 0x000000FF));

    return this;
  }

  @Override
  public final TCByteBuffer putUshort(int index, int s) {
    final int origPosition = position();

    try {
      position(index);
      putUshort(s);
    } finally {
      position(origPosition);
    }

    return this;
  }

  @Override
  public final long getUint() {
    long rv = 0;

    rv += ((long) (get() & 0xFF) << 24);
    rv += ((get() & 0xFF) << 16);
    rv += ((get() & 0xFF) << 8);
    rv += ((get() & 0xFF));

    return rv;
  }

  @Override
  public final long getUint(int index) {
    final int origPosition = position();

    try {
      position(index);
      return getUint();
    } finally {
      position(origPosition);
    }
  }

  @Override
  public final int getUshort() {
    int rv = 0;

    rv += ((get() & 0xFF) << 8);
    rv += ((get() & 0xFF));

    Assert.eval((rv >= 0) && (rv <= 0xFFFF));

    return rv;
  }

  @Override
  public final int getUshort(int index) {
    final int origPosition = position();

    try {
      position(index);
      return getUshort();
    } finally {
      position(origPosition);
    }
  }

  @Override
  public final short getUbyte() {
    return (short) (get() & 0xFF);
  }

  @Override
  public final short getUbyte(int index) {
    final int origPosition = position();

    try {
      position(index);
      return getUbyte();
    } finally {
      position(origPosition);
    }
  }

  @Override
  public final TCByteBuffer putUbyte(int index, short value) {
    final int origPosition = position();

    try {
      position(index);
      putUbyte(value);
    } finally {
      position(origPosition);
    }

    return this;
  }

  @Override
  public final TCByteBuffer putUbyte(short value) {
    if ((value < 0) || (value > 0xFF)) { throw new IllegalArgumentException(
                                                                            "Unsigned byte value must in range 0-255 inclusive"); }
    put((byte) (value & 0xFF));
    return this;
  }

  void verifyLocked() {
    if (ACCESS_CHECK && !lock.get()) {
      throw new IllegalStateException("buffer is not locked");
    }
  }

  private void checkLock() {
    if (ACCESS_CHECK && lock.get()) {
      throw new IllegalStateException("buffer is locked");
    }
  }

  private void lock() {
    if (ACCESS_CHECK && !lock.compareAndSet(false, true)) {
      throw new IllegalStateException("buffer is already locked");
    }
  }

  @Override
  public TCByteBuffer unlock() {
    if (ACCESS_CHECK && !lock.compareAndSet(true, false)) {
      throw new IllegalStateException("buffer is not locked");
    }
    return this;
  }

  private ByteBuffer accessBuffer() {
    checkLock();
    return hiddenBuffer;
  }
}
