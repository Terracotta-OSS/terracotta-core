/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.bytes;

/**
 * Factory for JDK14 style TCByteBuffer
 * 
 * @author teck
 */
final class TCByteBufferFactoryJDK14 implements TCByteBufferFactoryIF {

  final public TCByteBuffer getInstance(int capacity, boolean direct) {
    return new TCByteBufferJDK14(capacity, direct);
  }

  public TCByteBuffer wrap(byte[] data) {
    return TCByteBufferJDK14.wrap(data);
  }

}