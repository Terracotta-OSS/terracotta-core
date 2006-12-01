/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.bytes;

import com.tc.exception.ImplementMe;

/**
 * @author teck A JDK-1.3 compatible version of java.nio.ByteBuffer
 */
class TCByteBufferJDK13 extends AbstractTCByteBuffer {
  private final int     _offset;
  private final int     _capacity;
  private int           _limit;
  private int           _position;
  private final byte    _data[];
  private final boolean _readOnly;

  TCByteBufferJDK13(final int capacity, final boolean direct) {
    // "direct" parameter has no meaning here (yet)
    this(capacity);
  }

  private TCByteBufferJDK13(final int capacity) {
    this(new byte[capacity]);
  }

  TCByteBufferJDK13(byte[] data) {
    _data = data;
    _capacity = data.length;
    _limit = _capacity;
    _position = 0;
    _offset = 0;
    _readOnly = false;
  }

  private TCByteBufferJDK13(final byte data[], final int limit, final int position, final int offset, final boolean readOnly) {
    _offset = offset;
    _capacity = data.length - _offset;
    _data = data;
    _limit = limit;
    _position = position;
    _readOnly = readOnly;
  }

  public int capacity() {
    return _capacity;
  }

  public TCByteBuffer clear() {
    _limit = _data.length;
    _position = _offset;
    return this;
  }

  public int position() {
    return _position - _offset;
  }

  public TCByteBuffer flip() {
    _limit = _position;
    _position = _offset;
    return this;
  }

  public boolean hasRemaining() {
    return _position < _limit;
  }

  public int limit() {
    return _limit - _offset;
  }

  public TCByteBuffer limit(final int newLimit) {
    if ((newLimit > _capacity) || (newLimit < 0)) { throw new IllegalArgumentException(); }

    _limit = newLimit + _offset;
    if (_position > _limit) {
      _position = _limit;
    }

    return this;
  }

  public TCByteBuffer position(final int newPosition) {
    if ((newPosition > _limit) || (newPosition < 0)) throw new IllegalArgumentException();
    _position = newPosition + _offset;

    return this;
  }

  public int remaining() {
    return _limit - _position;
  }

  public TCByteBuffer rewind() {
    _position = _offset;
    return this;
  }

  public boolean isNioBuffer() {
    return false;
  }

  public Object getNioBuffer() {
    throw new UnsupportedOperationException("This buffer does not provide a backing java.nio.ByteBuffer");
  }

  public boolean isDirect() {
    return false;
  }

  public byte[] array() {
    return _data;
  }

  public byte get() {
    if (_position < _limit) {
      byte rv = _data[_position];
      _position++;
      return rv;
    }
    throw new TCBufferUnderflowException();
  }

  public TCByteBuffer get(final byte[] dst) {
    return get(dst, 0, dst.length);
  }

  public TCByteBuffer get(final byte[] dst, final int offset, final int length) {
    if (length > remaining()) { throw new TCBufferUnderflowException(); }

    System.arraycopy(_data, _position, dst, offset, length);
    _position += length;
    return this;
  }

  public byte get(final int index) {
    if ((index < 0) || (index > limit())) { throw new IndexOutOfBoundsException(); }

    return _data[index];
  }

  public int getInt() {
    throw new ImplementMe();
  }

  public long getLong() {
    throw new ImplementMe();
  }

  public TCByteBuffer put(final byte b) {
    if (_readOnly) { throw new TCReadOnlyBufferException(); }

    if (_position < _limit) {
      _data[_position] = b;
      _position++;
      return this;
    }

    throw new TCBufferOverflowException();
  }

  public TCByteBuffer put(final byte[] src) {
    if (_readOnly) { throw new TCReadOnlyBufferException(); }

    return put(src, 0, src.length);
  }

  public TCByteBuffer put(final byte[] src, final int offset, final int length) {
    if (_readOnly) { throw new TCReadOnlyBufferException(); }

    if (length > remaining()) { throw new TCBufferOverflowException(); }

    System.arraycopy(src, offset, _data, _position, length);
    _position += length;

    return this;
  }

  public TCByteBuffer put(final int index, final byte b) {
    if (_readOnly) { throw new TCReadOnlyBufferException(); }

    if ((index < 0) || (index > limit())) { throw new IndexOutOfBoundsException(); }

    _data[index] = b;

    return this;
  }

  public TCByteBuffer duplicate() {
    return new TCByteBufferJDK13(_data, _limit, _position, _offset, _readOnly);
  }

  public TCByteBuffer put(TCByteBuffer src) {
    if (src.remaining() > remaining()) { throw new TCBufferOverflowException(); }

    if (src == this) { throw new IllegalArgumentException("Source buffer can not be this buffer"); }

    if (_readOnly) { throw new TCReadOnlyBufferException(); }

    int length = src.remaining();

    System.arraycopy(src.array(), src.position() + src.arrayOffset(), _data, _position, length);
    src.position(src.position() + length);

    _position += length;

    return this;
  }

  public TCByteBuffer putInt(int i) {

    throw new ImplementMe();
  }

  public TCByteBuffer putLong(long l) {

    throw new ImplementMe();
  }

  public TCByteBuffer slice() {
    final int newLimit = _position + remaining();

    return new TCByteBufferJDK13(_data, newLimit, _position, _position, _readOnly);
  }

  public int arrayOffset() {
    return _offset;
  }

  public TCByteBuffer asReadOnlyBuffer() {
    return new TCByteBufferJDK13(_data, _limit, _position, _offset, true);
  }

  public boolean isReadOnly() {
    return _readOnly;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.tc.bytes.TCByteBuffer#putChar(char)
   */
  public TCByteBuffer putChar(char c) {

    throw new ImplementMe();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.tc.bytes.TCByteBuffer#putFloat(float)
   */
  public TCByteBuffer putFloat(float f) {

    throw new ImplementMe();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.tc.bytes.TCByteBuffer#putDouble(double)
   */
  public TCByteBuffer putDouble(double d) {

    throw new ImplementMe();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.tc.bytes.TCByteBuffer#put(int, char)
   */
  public TCByteBuffer putChar(int index, char c) {

    throw new ImplementMe();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.tc.bytes.TCByteBuffer#putInt(int, int)
   */
  public TCByteBuffer putInt(int index, int i) {

    throw new ImplementMe();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.tc.bytes.TCByteBuffer#putLong(int, long)
   */
  public TCByteBuffer putLong(int index, long l) {

    throw new ImplementMe();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.tc.bytes.TCByteBuffer#putFloat(int, float)
   */
  public TCByteBuffer putFloat(int index, float f) {

    throw new ImplementMe();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.tc.bytes.TCByteBuffer#putDouble(int, double)
   */
  public TCByteBuffer putDouble(int index, double d) {

    throw new ImplementMe();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.tc.bytes.TCByteBuffer#getBoolean()
   */
  public boolean getBoolean() {

    throw new ImplementMe();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.tc.bytes.TCByteBuffer#getBoolean(int)
   */
  public boolean getBoolean(int index) {

    throw new ImplementMe();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.tc.bytes.TCByteBuffer#getChar()
   */
  public char getChar() {

    throw new ImplementMe();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.tc.bytes.TCByteBuffer#getChar(int)
   */
  public char getChar(int index) {

    throw new ImplementMe();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.tc.bytes.TCByteBuffer#getDouble()
   */
  public double getDouble() {

    throw new ImplementMe();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.tc.bytes.TCByteBuffer#getDouble(int)
   */
  public double getDouble(int index) {

    throw new ImplementMe();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.tc.bytes.TCByteBuffer#getFloat()
   */
  public float getFloat() {

    throw new ImplementMe();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.tc.bytes.TCByteBuffer#getFloat(int)
   */
  public float getFloat(int index) {

    throw new ImplementMe();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.tc.bytes.TCByteBuffer#getInt(int)
   */
  public int getInt(int index) {

    throw new ImplementMe();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.tc.bytes.TCByteBuffer#getLong(int)
   */
  public long getLong(int index) {

    throw new ImplementMe();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.tc.bytes.TCByteBuffer#getShort()
   */
  public short getShort() {

    throw new ImplementMe();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.tc.bytes.TCByteBuffer#getShort(int)
   */
  public short getShort(int index) {

    throw new ImplementMe();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.tc.bytes.TCByteBuffer#putBoolean(boolean)
   */
  public TCByteBuffer putBoolean(boolean b) {

    throw new ImplementMe();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.tc.bytes.TCByteBuffer#putBoolean(int, boolean)
   */
  public TCByteBuffer putBoolean(int index, boolean b) {

    throw new ImplementMe();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.tc.bytes.TCByteBuffer#putShort(short)
   */
  public TCByteBuffer putShort(short s) {

    throw new ImplementMe();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.tc.bytes.TCByteBuffer#putShort(int, short)
   */
  public TCByteBuffer putShort(int index, short s) {

    throw new ImplementMe();
  }

  public boolean hasArray() {
    return true;
  }

  public void recycle() {
    // no op
  }

}