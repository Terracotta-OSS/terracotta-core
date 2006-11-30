/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
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