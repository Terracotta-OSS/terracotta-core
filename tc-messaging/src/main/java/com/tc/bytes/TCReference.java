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
package com.tc.bytes;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 *
 */
public interface TCReference extends Iterable<TCByteBuffer>, AutoCloseable {
  
  TCReference duplicate();
  
  default TCReference duplicate(int length) {
    return duplicate().truncate(length);
  }
  
  default TCReference truncate(int length) {
    Iterator<TCByteBuffer> it = iterator();
    int runTo = length;
    while (it.hasNext()) {
      TCByteBuffer curs = it.next();
      curs.limit(Math.min(curs.position() + runTo, curs.limit()));
      runTo -= curs.remaining();
    }
    return this;
  }

  default TCReference limit(int length) {
    Iterator<TCByteBuffer> it = iterator();
    int runTo = length;
    while (it.hasNext()) {
      TCByteBuffer curs = it.next();
      int min = Math.min(runTo, curs.capacity());
      curs.limit(min);
      runTo -= curs.limit();
    }
    return this;
  }

  default long length() {
    Iterator<TCByteBuffer> it = iterator();
    int runTo = 0;
    while (it.hasNext()) {
      TCByteBuffer curs = it.next();
      if (curs.hasRemaining()) {
        runTo += curs.position();
        break;
      } else {
        runTo += curs.limit();
      }
    }
    return runTo;
  }
  
  default long available() {
    return stream().map(TCByteBuffer::remaining).map(v->(long)v).reduce(0L, Long::sum);
  }

  default boolean hasRemaining() {
    return stream().anyMatch(TCByteBuffer::hasRemaining);
  }
  
  default ByteBuffer[] toByteBufferArray() {
    return stream().map(TCByteBuffer::getNioBuffer).toArray(ByteBuffer[]::new);
  }
  
  default TCByteBuffer[] toArray() {
    return stream().toArray(TCByteBuffer[]::new);
  }
  
  default Stream<TCByteBuffer> stream() {
    return StreamSupport.stream(spliterator(), false);
  }
  
  default void returnByteBufferArray(ByteBuffer[] array) {
    TCByteBuffer[] src = toArray();
    for (int x=0;x<src.length;x++) {
      src[x].returnNioBuffer(array[x]);
    }
  }
  
  @Override
  public void close();
}
