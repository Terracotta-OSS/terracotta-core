/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
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

import com.tc.util.concurrent.SetOnceFlag;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.function.Supplier;

/**
 *
 */
public class TCByteBufferAllocator {
  private final Supplier<TCByteBuffer> newBuffers;
  private final Queue<TCByteBuffer> returns;
  private final List<TCByteBuffer> items = new ArrayList<>();
  private final SetOnceFlag complete = new SetOnceFlag();
  
  public TCByteBufferAllocator(Supplier<TCByteBuffer> tracked) {
    this(tracked, new LinkedList<>());
  }
  
  public TCByteBufferAllocator(Supplier<TCByteBuffer> tracked, Queue<TCByteBuffer> returns) {
    this.newBuffers = tracked;
    this.returns = returns;
  }
  
  public TCByteBuffer add() {
    if (complete.isSet()) {
      throw new IllegalStateException("buffers already accessed");
    }
    TCByteBuffer next = returns.poll();
    if (next == null) {
      next = newBuffers.get();
    }
    items.add(next);
    return next;
  }
  
  private void reset(int stop) {
    if (complete.isSet()) {
      throw new IllegalStateException("buffers already accessed");
    }
    Iterator<TCByteBuffer> it = items.iterator();
    int pos = 0;
    while (it.hasNext()) {
      TCByteBuffer c = it.next();
      if (c.position() + pos < stop) {
        pos += c.position();
      } else if (pos < stop) {
        c.position(stop - pos);
      } else {
        it.remove();
        returns.add(c.reInit());
      }
    }
  }
  
  public void rewind(int r) {
    int len = items.stream().mapToInt(TCByteBuffer::position).sum();
    reset(len - r);
  }
  
  public TCReference complete() {
    complete.set();
    return TCReferenceSupport.createReference(items, returns::add);
  }
}
