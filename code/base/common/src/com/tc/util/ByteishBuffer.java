/**
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.util;

/**
 * Stuff common to ByteBuffer and TCByteBuffer.
 */
interface ByteishBuffer {

  byte get(int position);

  int limit();

}