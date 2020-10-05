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
import com.tc.util.State;

import java.nio.ByteBuffer;
import java.util.Queue;

/**
 * @author teck A thin wrapper to a real java.nio.ByteBuffer instance
 */

// XXX: Should we wrap the native java.nio overflow, underflow and readOnly exceptions with the TC versions?
// This would make the TCByteBuffer interface consistent w.r.t. exceptions (whilst being blind to JDK13 vs JDK14)
public class TCByteBufferImpl implements TCByteBuffer, BufferPool {

  private static final State        INIT        = new State("INIT");
  private static final State        CHECKED_OUT = new State("CHECKED_OUT");
  private static final State        COMMITTED   = new State("COMMITTED");

  private final ByteBuffer          buffer;
  private final TCByteBuffer        root;
  private final Queue<TCByteBuffer> bufPool;
  private State                     state       = INIT;
  private final boolean isReadOnly;
  
  TCByteBufferImpl(int capacity, boolean direct, Queue<TCByteBuffer> poolQueue) {
    if (direct) {
      buffer = ByteBuffer.allocateDirect(capacity);
    } else {
      buffer = ByteBuffer.allocate(capacity);
    }
    bufPool = poolQueue;
    root = this;
    isReadOnly = false;
  }

  private TCByteBufferImpl(ByteBuffer buf) {
    buffer = buf;
    bufPool = null;
    this.root = null;
    this.isReadOnly = false;
  }

  private TCByteBufferImpl(ByteBuffer buf, TCByteBuffer root, boolean isReadOnly) {
    buffer = buf;
    bufPool = null;
    this.root = root;
    this.isReadOnly = isReadOnly;
  }

  static TCByteBuffer wrap(byte[] data) {
    return new TCByteBufferImpl(ByteBuffer.wrap(data));
  }

  protected ByteBuffer getBuffer() {
    return buffer;
  }

  @Override
  public TCByteBuffer clear() {
    buffer.clear();
    return this;
  }

  @Override
  public int capacity() {
    return buffer.capacity();
  }

  @Override
  public int position() {
    return buffer.position();
  }

  @Override
  public TCByteBuffer flip() {
    buffer.flip();
    return this;
  }

  @Override
  public boolean hasRemaining() {
    return buffer.hasRemaining();
  }

  @Override
  public int limit() {
    return buffer.limit();
  }

  @Override
  public TCByteBuffer limit(int newLimit) {
    buffer.limit(newLimit);
    return this;
  }

  @Override
  public TCByteBuffer position(int newPosition) {
    buffer.position(newPosition);
    return this;
  }

  @Override
  public int remaining() {
    return buffer.remaining();
  }

  @Override
  public com.tc.bytes.TCByteBuffer rewind() {
    buffer.rewind();
    return this;
  }

  @Override
  public ByteBuffer getNioBuffer() {
    return buffer;
  }

  @Override
  public boolean isDirect() {
    return buffer.isDirect();
  }

  @Override
  public byte[] array() {
    return buffer.array();
  }

  @Override
  public byte get() {
    return buffer.get();
  }

  @Override
  public boolean getBoolean() {
    // XXX: Um-- why isn't there a getBoolean in ByteBuffer?
    return buffer.get() > 0;
  }

  @Override
  public boolean getBoolean(int index) {
    return buffer.get(index) > 0;
  }

  @Override
  public char getChar() {
    return buffer.getChar();
  }

  @Override
  public char getChar(int index) {
    return buffer.getChar(index);
  }

  @Override
  public double getDouble() {
    return buffer.getDouble();
  }

  @Override
  public double getDouble(int index) {
    return buffer.getDouble(index);
  }

  @Override
  public float getFloat() {
    return buffer.getFloat();
  }

  @Override
  public float getFloat(int index) {
    return buffer.getFloat(index);
  }

  @Override
  public int getInt() {
    return buffer.getInt();
  }

  @Override
  public int getInt(int index) {
    return buffer.getInt(index);
  }

  @Override
  public long getLong() {
    return buffer.getLong();
  }

  @Override
  public long getLong(int index) {
    return buffer.getLong(index);
  }

  @Override
  public short getShort() {
    return buffer.getShort();
  }

  @Override
  public short getShort(int index) {
    return buffer.getShort(index);
  }

  @Override
  public TCByteBuffer get(byte[] dst) {
    buffer.get(dst);
    return this;
  }

  @Override
  public TCByteBuffer get(byte[] dst, int offset, int length) {
    buffer.get(dst, offset, length);
    return this;
  }

  @Override
  public byte get(int index) {
    return buffer.get(index);
  }

  @Override
  public TCByteBuffer put(byte b) {
    buffer.put(b);
    return this;
  }

  @Override
  public TCByteBuffer put(byte[] src) {
    buffer.put(src);
    return this;
  }

  @Override
  public TCByteBuffer put(byte[] src, int offset, int length) {
    buffer.put(src, offset, length);
    return this;
  }

  @Override
  public TCByteBuffer put(int index, byte b) {
    buffer.put(index, b);
    return this;
  }

  @Override
  public TCByteBuffer putBoolean(boolean b) {
    // XXX: Why isn't there a putBoolean in ByteBuffer?
    buffer.put((b) ? (byte) 1 : (byte) 0);
    return this;
  }

  @Override
  public TCByteBuffer putBoolean(int index, boolean b) {
    buffer.put(index, (b) ? (byte) 1 : (byte) 0);
    return this;
  }

  @Override
  public TCByteBuffer putChar(char c) {
    buffer.putChar(c);
    return this;
  }

  @Override
  public TCByteBuffer putChar(int index, char c) {
    buffer.putChar(index, c);
    return this;
  }

  @Override
  public TCByteBuffer putDouble(double d) {
    buffer.putDouble(d);
    return this;
  }

  @Override
  public TCByteBuffer putDouble(int index, double d) {
    buffer.putDouble(index, d);
    return this;
  }

  @Override
  public TCByteBuffer putFloat(float f) {
    buffer.putFloat(f);
    return this;
  }

  @Override
  public TCByteBuffer putFloat(int index, float f) {
    buffer.putFloat(index, f);
    return this;
  }

  @Override
  public TCByteBuffer putInt(int i) {
    buffer.putInt(i);
    return this;
  }

  @Override
  public TCByteBuffer putInt(int index, int i) {
    buffer.putInt(index, i);
    return this;
  }

  @Override
  public TCByteBuffer putLong(long l) {
    buffer.putLong(l);
    return this;
  }

  @Override
  public TCByteBuffer putLong(int index, long l) {
    buffer.putLong(index, l);
    return this;
  }

  @Override
  public TCByteBuffer putShort(short s) {
    buffer.putShort(s);
    return this;
  }

  @Override
  public TCByteBuffer putShort(int index, short s) {
    buffer.putShort(index, s);
    return this;
  }

  @Override
  public TCByteBuffer duplicate() {
    return new TCByteBufferImpl(buffer.duplicate(), root, false);
  }

  @Override
  public TCByteBuffer put(TCByteBuffer src) {
    buffer.put(src.getNioBuffer());
    return this;
  }

  @Override
  public TCByteBuffer slice() {
    return new TCByteBufferImpl(buffer.slice(), root, false);
  }

  @Override
  public int arrayOffset() {
    return buffer.arrayOffset();
  }

  @Override
  public TCByteBuffer asReadOnlyBuffer() {
    return new TCByteBufferImpl(buffer.duplicate(), root, true);
  }

  @Override
  public boolean isReadOnly() {
    return buffer.isReadOnly();
  }

  @Override
  public String toString() {
    return (buffer == null) ? "TCByteBufferJDK14(null buffer)" : "TCByteBufferJDK14@" + System.identityHashCode(this)
                                                                 + "(" + buffer.toString() + ")";
  }

  @Override
  public boolean hasArray() {
    return buffer.hasArray();
  }

  // Can be called only once on any of the views and the root is gone
  @Override
  public void recycle() {
    if (root != null) {
      TCByteBufferFactory.returnBuffer(root.reInit());
    }
  }

  @Override
  public TCByteBuffer reInit() {
    clear();
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

  @Override
  public void commit() {
    if (state == COMMITTED) { throw new AssertionError("Already commited"); }
    state = COMMITTED;
  }

  @Override
  public void checkedOut() {
    if (state == CHECKED_OUT) { throw new AssertionError("Already checked out"); }
    state = CHECKED_OUT;
  }

  @Override
  public BufferPool getBufferPool() {
    return this;
  }

  @Override
  public void offer(TCByteBuffer buf) throws InterruptedException {
    if (this.bufPool != null) {
      this.bufPool.offer(buf);
    }
  }

  /* This is the debug version. PLEASE DONT DELETE */

  //
  // private final ByteBuffer buffer;
  // private final TCByteBufferJDK14 root;
  // private List childs;
  // private static final boolean debug = true;
  // private static final boolean debugFinalize = false;
  // private ActivityMonitor monitor;
  //
  // TCByteBufferJDK14(int capacity, boolean direct) {
  // if (direct) {
  // buffer = ByteBuffer.allocateDirect(capacity);
  // } else {
  // buffer = ByteBuffer.allocate(capacity);
  // }
  // root = this;
  // if (debug) {
  // childs = new ArrayList();
  // monitor = new ActivityMonitor();
  // monitor.addActivity("TCBB", "Created");
  // }
  // }
  //
  // private TCByteBufferJDK14(ByteBuffer buf) {
  // buffer = buf;
  // this.root = null;
  // if (debug) childs = new ArrayList();
  // }
  //
  // private TCByteBufferJDK14(ByteBuffer buf, TCByteBufferJDK14 root) {
  // buffer = buf;
  // childs = null;
  // this.root = root;
  // if (debug) this.root.addChild(this);
  // }
  //
  // private void addChild(TCByteBufferJDK14 child) {
  // if (debug) childs.add(child);
  // }
  //
  // static TCByteBufferJDK14 wrap(byte[] data) {
  // return new TCByteBufferJDK14(ByteBuffer.wrap(data));
  // }
  //
  // protected ByteBuffer getBuffer() {
  // if (debug) {
  // checkState();
  // logGet();
  // }
  // return buffer;
  // }
  //
  // public TCByteBuffer clear() {
  // buffer.clear();
  // if (debug) {
  // childs.clear();
  // monitor.clear();
  // }
  // return this;
  // }
  //
  // public int capacity() {
  // if (debug) checkState();
  // return buffer.capacity();
  // }
  //
  // public int position() {
  // if (debug) checkState();
  // return buffer.position();
  // }
  //
  // public TCByteBuffer flip() {
  // if (debug) checkState();
  // buffer.flip();
  // return this;
  // }
  //
  // private void checkState() {
  // if (debug && this != root) {
  // // This doesnt check for the root itself, I dont know how to check for the root itself being modified once check
  // // back in
  // Assert.assertNotNull(root);
  // Assert.assertTrue(root.isChild(this));
  // }
  // }
  //
  // private boolean isChild(TCByteBufferJDK14 child) {
  // return childs.contains(child);
  // }
  //
  // public boolean hasRemaining() {
  // if (debug) checkState();
  // return buffer.hasRemaining();
  // }
  //
  // public int limit() {
  // if (debug) checkState();
  // return buffer.limit();
  // }
  //
  // public TCByteBuffer limit(int newLimit) {
  // if (debug) checkState();
  // buffer.limit(newLimit);
  // return this;
  // }
  //
  // public TCByteBuffer position(int newPosition) {
  // if (debug) checkState();
  // buffer.position(newPosition);
  // return this;
  // }
  //
  // public int remaining() {
  // if (debug) checkState();
  // return buffer.remaining();
  // }
  //
  // public com.tc.bytes.TCByteBuffer rewind() {
  // if (debug) checkState();
  // buffer.rewind();
  // return this;
  // }
  //
  // public boolean isNioBuffer() {
  // return true;
  // }
  //
  // public Object getNioBuffer() {
  // if (debug) checkState();
  // return buffer;
  // }
  //
  // public boolean isDirect() {
  // return buffer.isDirect();
  // }
  //
  // public byte[] array() {
  // // Not fool proof
  // if (debug) checkState();
  // return buffer.array();
  // }
  //
  // public byte get() {
  // if (debug) {
  // checkState();
  // logGet();
  // }
  // return buffer.get();
  // }
  //
  // public boolean getBoolean() {
  // // XXX: Um-- why isn't there a getBoolean in ByteBuffer?
  // if (debug) {
  // checkState();
  // logGet();
  // }
  // return buffer.get() > 0;
  // }
  //
  // public boolean getBoolean(int index) {
  // if (debug) {
  // checkState();
  // logGet();
  // }
  // return buffer.get(index) > 0;
  // }
  //
  // public char getChar() {
  // if (debug) {
  // checkState();
  // logGet();
  // }
  // return buffer.getChar();
  // }
  //
  // public char getChar(int index) {
  // if (debug) {
  // checkState();
  // logGet();
  // }
  // return buffer.getChar(index);
  // }
  //
  // public double getDouble() {
  // if (debug) {
  // checkState();
  // logGet();
  // }
  // return buffer.getDouble();
  // }
  //
  // public double getDouble(int index) {
  // if (debug) {
  // checkState();
  // logGet();
  // }
  // return buffer.getDouble(index);
  // }
  //
  // public float getFloat() {
  // if (debug) {
  // checkState();
  // logGet();
  // }
  // return buffer.getFloat();
  // }
  //
  // public float getFloat(int index) {
  // if (debug) {
  // checkState();
  // logGet();
  // }
  // return buffer.getFloat(index);
  // }
  //
  // public int getInt() {
  // if (debug) {
  // checkState();
  // logGet();
  // }
  // return buffer.getInt();
  // }
  //
  // public int getInt(int index) {
  // if (debug) {
  // checkState();
  // logGet();
  // }
  // return buffer.getInt(index);
  // }
  //
  // public long getLong() {
  // if (debug) {
  // checkState();
  // logGet();
  // }
  // return buffer.getLong();
  // }
  //
  // public long getLong(int index) {
  // if (debug) {
  // checkState();
  // logGet();
  // }
  // return buffer.getLong(index);
  // }
  //
  // public short getShort() {
  // if (debug) {
  // checkState();
  // logGet();
  // }
  // return buffer.getShort();
  // }
  //
  // public short getShort(int index) {
  // if (debug) {
  // checkState();
  // logGet();
  // }
  // return buffer.getShort(index);
  // }
  //
  // public TCByteBuffer get(byte[] dst) {
  // if (debug) {
  // checkState();
  // logGet();
  // }
  // buffer.get(dst);
  // return this;
  // }
  //
  // public TCByteBuffer get(byte[] dst, int offset, int length) {
  // if (debug) {
  // checkState();
  // logGet();
  // }
  // buffer.get(dst, offset, length);
  // return this;
  // }
  //
  // public byte get(int index) {
  // if (debug) {
  // checkState();
  // logGet();
  // }
  // return buffer.get(index);
  // }
  //
  // public TCByteBuffer put(byte b) {
  // if (debug) {
  // checkState();
  // logPut();
  // }
  // buffer.put(b);
  // return this;
  // }
  //
  // public TCByteBuffer put(byte[] src) {
  // if (debug) {
  // checkState();
  // logPut();
  // }
  // buffer.put(src);
  // return this;
  // }
  //
  // public TCByteBuffer put(byte[] src, int offset, int length) {
  // if (debug) {
  // checkState();
  // logPut();
  // }
  // buffer.put(src, offset, length);
  // return this;
  // }
  //
  // public TCByteBuffer put(int index, byte b) {
  // if (debug) {
  // checkState();
  // logPut();
  // }
  // buffer.put(index, b);
  // return this;
  // }
  //
  // public TCByteBuffer putBoolean(boolean b) {
  // // XXX: Why isn't there a putBoolean in ByteBuffer?
  // if (debug) {
  // checkState();
  // logPut();
  // }
  // buffer.put((b) ? (byte) 1 : (byte) 0);
  // return this;
  // }
  //
  // public TCByteBuffer putBoolean(int index, boolean b) {
  // if (debug) {
  // checkState();
  // logPut();
  // }
  // buffer.put(index, (b) ? (byte) 1 : (byte) 0);
  // return this;
  // }
  //
  // public TCByteBuffer putChar(char c) {
  // if (debug) {
  // checkState();
  // logPut();
  // }
  // buffer.putChar(c);
  // return this;
  // }
  //
  // public TCByteBuffer putChar(int index, char c) {
  // if (debug) {
  // checkState();
  // logPut();
  // }
  // buffer.putChar(index, c);
  // return this;
  // }
  //
  // public TCByteBuffer putDouble(double d) {
  // if (debug) {
  // checkState();
  // logPut();
  // }
  // buffer.putDouble(d);
  // return this;
  // }
  //
  // public TCByteBuffer putDouble(int index, double d) {
  // if (debug) {
  // checkState();
  // logPut();
  // }
  // buffer.putDouble(index, d);
  // return this;
  // }
  //
  // public TCByteBuffer putFloat(float f) {
  // if (debug) {
  // checkState();
  // logPut();
  // }
  // buffer.putFloat(f);
  // return this;
  // }
  //
  // public TCByteBuffer putFloat(int index, float f) {
  // if (debug) {
  // checkState();
  // logPut();
  // }
  // buffer.putFloat(index, f);
  // return this;
  // }
  //
  // public TCByteBuffer putInt(int i) {
  // if (debug) {
  // checkState();
  // logPut();
  // }
  // buffer.putInt(i);
  // return this;
  // }
  //
  // public TCByteBuffer putInt(int index, int i) {
  // if (debug) {
  // checkState();
  // logPut();
  // }
  // buffer.putInt(index, i);
  // return this;
  // }
  //
  // public TCByteBuffer putLong(long l) {
  // if (debug) {
  // checkState();
  // logPut();
  // }
  // buffer.putLong(l);
  // return this;
  // }
  //
  // public TCByteBuffer putLong(int index, long l) {
  // if (debug) checkState();
  // logPut();
  // buffer.putLong(index, l);
  // return this;
  // }
  //
  // public TCByteBuffer putShort(short s) {
  // if (debug) {
  // checkState();
  // logPut();
  // }
  // buffer.putShort(s);
  // return this;
  // }
  //
  // public TCByteBuffer putShort(int index, short s) {
  // if (debug) {
  // checkState();
  // logPut();
  // }
  // buffer.putShort(index, s);
  // return this;
  // }
  //
  // public TCByteBuffer duplicate() {
  // if (debug) checkState();
  // return new TCByteBufferJDK14(buffer.duplicate(), root);
  // }
  //
  // public TCByteBuffer put(TCByteBuffer src) {
  // if (debug) {
  // checkState();
  // logPut();
  // }
  //
  // if (!src.isNioBuffer()) { throw new IllegalArgumentException("src buffer is not backed by a java.nio.ByteBuffer");
  // }
  //
  // buffer.put((ByteBuffer) src.getNioBuffer());
  // return this;
  // }
  //
  // public TCByteBuffer slice() {
  // if (debug) checkState();
  // return new TCByteBufferJDK14(buffer.slice(), root);
  // }
  //
  // public int arrayOffset() {
  // if (debug) checkState();
  // return buffer.arrayOffset();
  // }
  //
  // public TCByteBuffer asReadOnlyBuffer() {
  // if (debug) checkState();
  // return new TCByteBufferJDK14(buffer.asReadOnlyBuffer(), root);
  // }
  //
  // public boolean isReadOnly() {
  // if (debug) checkState();
  // return buffer.isReadOnly();
  // }
  //
  // public String toString() {
  // if (debug) checkState();
  // return (buffer == null) ? "null buffer" : buffer.toString();
  // }
  //
  // public boolean hasArray() {
  // if (debug) checkState();
  // return buffer.hasArray();
  // }
  //
  // // Can be called only once on any of the views and the root is gone
  // public void recycle() {
  // if (debug) checkState();
  // if(root != null) TCByteBufferFactory.returnBuffer(root.reInit());
  // }
  //
  // private TCByteBufferJDK14 reInit() {
  // clear();
  // return this;
  // }
  //
  // void logGet() {
  // if(root !=null) {
  // root.monitor.clear();
  // root.monitor.addActivity("TCBB", "get");
  // }
  // }
  //
  // void logPut() {
  // if(root !=null) {
  // root.monitor.clear();
  // root.monitor.addActivity("TCBB", "put");
  // }
  // }
  //
  // static int count;
  // static {
  // CommonShutDownHook.addShutdownHook(new Runnable() {
  // public void run() {
  // logger.info("No of Root Buffers finalized = " + count);
  // }
  // });
  // }
  //
  // public void finalize() {
  // if (this == root) {
  // count++;
  // if (debugFinalize) monitor.printActivityFor("TCBB");
  // }
  // }
}
