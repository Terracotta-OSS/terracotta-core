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
package com.tc.io;

import com.tc.bytes.TCByteBuffer;

public interface TCByteBufferInput extends TCDataInput {

  public interface Mark {
    // This is just a Marker interface as far as anyone is concerned
  }

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
  public TCByteBufferInput duplicateAndLimit(int limit);

  public TCByteBuffer[] toArray();

  public TCByteBuffer[] toArray(Mark start, Mark end);

  /**
   * Artificially limit the length of this input stream starting at the current read position. This operation is
   * destructive to the stream contents (ie. data trimmed off by setting limit can never be read with this stream).
   */
  public TCDataInput limit(int limit);

  public int getTotalLength();

  public int available();

  public void close();

  public void mark(int readlimit);

  /**
   * This is a TC special version of mark() to be used in conjunction with tcReset()...We should eventually implement
   * the general purpose mark(int) method as specified by InputStream. NOTE: It has some unusual semantics that make it
   * a little trickier to implement (in our case) than you might think (specifically the readLimit field)
   */
  public Mark mark();

  public boolean markSupported();

  public int read(byte[] b);

  public int read();

  public void reset();

  /**
   * Reset this input stream to the position recorded by the mark that is passed an input parameter.
   * 
   * @throws IllegalArgumentException if m is null or if it was not created against this stream.
   */
  public void tcReset(Mark m);

  public long skip(long skip);

}