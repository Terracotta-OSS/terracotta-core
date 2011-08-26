/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util;

/**
 * Stuff common to ByteBuffer and TCByteBuffer.
 */
interface ByteishBuffer {

  byte get(int position);

  int limit();

}