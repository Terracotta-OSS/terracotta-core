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
package com.tc.util;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 *
 */
public class SimpleRingBuffer<T> implements Iterable<T> {
  
  private final Object[] buffer;
  private int head = 0;

  public SimpleRingBuffer(int size) {
    buffer = new Object[size];
    head = 0;
  }
  
  public void put(T item) {
    Objects.nonNull(item);
    buffer[head++] = item;
    head = head % buffer.length;
  }
  
  public Stream<T> stream() {
    return StreamSupport.stream(spliterator(), false);
  }

  @Override
  public Iterator<T> iterator() {
    final Object[] ref = (buffer[head] != null) ? Arrays.copyOf(buffer, buffer.length) : Arrays.copyOf(buffer, head);
    final int end = head % ref.length;
    
    return new Iterator<T>() {
      private int pos = (end + 1) % ref.length;
      
      @Override
      public boolean hasNext() {
        return pos != end;
      }

      @Override
      @SuppressWarnings("unchecked")
      public T next() {
        try {
          return (T)ref[pos++];
        } finally {
          pos = pos % ref.length;
        }
      }
    };
  }
}
