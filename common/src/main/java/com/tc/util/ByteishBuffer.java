/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util;

/**
 * Stuff common to ByteBuffer and TCByteBuffer.
 */
interface ByteishBuffer {

  byte get(int position);

  int limit();

}
