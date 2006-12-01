/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.bytes;

/**
 * Factory for JDK13 style TCByteBuffer
 * 
 * @author teck
 */
class TCByteBufferFactoryJDK13 implements TCByteBufferFactoryIF {

  final public TCByteBuffer getInstance(int capacity, boolean direct) {
    return new TCByteBufferJDK13(capacity, direct);
  }

  public TCByteBuffer wrap(byte[] data) {
    return new TCByteBufferJDK13(data);
  }
}