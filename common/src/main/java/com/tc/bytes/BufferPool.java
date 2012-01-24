/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.bytes;

public interface BufferPool {

  void offer(TCByteBuffer buf) throws InterruptedException;

}
