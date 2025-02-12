/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
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
  
  default long available() {
    return stream().mapToInt(TCByteBuffer::remaining).asLongStream().sum();
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
