/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.io;

import com.tc.bytes.TCByteBuffer;

import java.io.IOException;

public interface TCByteBufferInput  extends TCDataInput {

  /**
   * Duplicate this stream. The resulting stream will share data with the source stream (ie. no copying), but the two
   * streams will have independent read positions. The read position of the result stream will initially be the same as
   * the source stream
   */
  public TCByteBufferInput duplicate();

  /**
   * Effectively the same thing as calling duplicate().limit(int), but potentially creating far less garbage (depending
   * on the size difference between the original stream and the slice you want)
   */
  public TCByteBufferInput duplicateAndLimit(final int limit);

  public TCByteBuffer[] toArray();

  /**
   * Artificially limit the length of this input stream starting at the current read position. This operation is
   * destructive to the stream contents (ie. data trimmed off by setting limit can never be read with this stream).
   */
  public TCDataInput limit(int limit);

  public int getTotalLength();

  public int available();

  public void close();

  public void mark(int readlimit);

  // XXX: This is a TC special version of mark() to be used in conjunction with tcReset()...We should eventually
  // implement the general purpose mark(int) method as specified by InputStream. NOTE: It has some unusual semantics
  // that make it a little trickier to implement (in our case) than you might think (specifially the readLimit field)
  public void mark();

  public boolean markSupported();

  public int read(byte[] b);

  public int read();

  public void reset();

  /**
   * Reset this input stream to the position recorded by the last call to mark(). This method discards the previous
   * value of the mark
   * 
   * @throws IOException if mark() has never been called on this stream
   */
  public void tcReset();

  public long skip(long skip);

}