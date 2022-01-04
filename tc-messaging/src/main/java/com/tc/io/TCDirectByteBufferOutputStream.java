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
import com.tc.bytes.TCByteBufferFactory;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Use me to write data to a set of TCByteBuffer instances. <br>
 * <br>
 * NOTE: This class never throws java.io.IOException (unlike the generic OutputStream) class
 */
public class TCDirectByteBufferOutputStream extends TCByteBufferOutputStream {

  private final Queue<TCByteBuffer> cache;

  public TCDirectByteBufferOutputStream() {
    this(new LinkedList<>());
  }

  public TCDirectByteBufferOutputStream(Queue<TCByteBuffer> cache) {
    this.cache = cache;
  }

  @Override
  protected TCByteBuffer newBuffer() {
    TCByteBuffer bb = cache.poll();
    if (bb == null) {
      bb = TCByteBufferFactory.getDirectByteBuffer();
    }
    return bb;
  }

  @Override
  public void reset() {
    close();
    for (TCByteBuffer buffer : toArray()) {
      cache.offer(buffer.reInit());
    }
    super.reset();
  }
}

