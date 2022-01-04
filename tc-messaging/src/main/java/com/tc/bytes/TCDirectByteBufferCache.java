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

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.AbstractQueue;
import java.util.Deque;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 *
 */
public class TCDirectByteBufferCache extends AbstractQueue<TCByteBuffer> {
  private final ReferenceQueue<TCByteBuffer> gcqueue = new ReferenceQueue<>();
  private final Set<Reference<? extends TCByteBuffer>> refs = ConcurrentHashMap.newKeySet();
  private final TCDirectByteBufferCache parent;

  private final int size;
  private final int limit;
  private final Deque<TCByteBuffer> localpool = new ConcurrentLinkedDeque<>();

  public TCDirectByteBufferCache() {
    this(TCByteBufferFactory.getFixedBufferSize());
  }

  public TCDirectByteBufferCache(int size) {
    this(null, size, Integer.MAX_VALUE);
  }

  public TCDirectByteBufferCache(TCDirectByteBufferCache parent) {
    this(parent, TCByteBufferFactory.getFixedBufferSize(), Integer.MAX_VALUE);
  }
  
  public TCDirectByteBufferCache(TCDirectByteBufferCache parent, int size, int limit) {
    this.parent = parent;
    this.size = size;
    this.limit = limit;
  }

  private void processReferencePool() {
    Reference<? extends TCByteBuffer> ref = gcqueue.poll();
    while (ref != null) {
      TCByteBuffer buf = ref.get();
      if (buf != null) {
        localpool.offer(buf.reInit());
      } else {
        refs.remove(ref);
      }
      ref = gcqueue.poll();
    }
  }

  @Override
  public Iterator<TCByteBuffer> iterator() {
    return localpool.iterator();
  }

  @Override
  public int size() {
    return localpool.size();
  }

  @Override
  public boolean offer(TCByteBuffer e) {
    if (e instanceof TCByteBufferImpl) {
      ((TCByteBufferImpl)e).verifyLocked();
    }
    return (localpool.size() > limit) ? false : localpool.offerFirst(e);
  }

  @Override
  public TCByteBuffer poll() {
    processReferencePool();
    TCByteBuffer buffer = localpool.pollLast();
    if (buffer == null) {
      if (parent != null) {
        buffer = parent.poll();
      } else {
        buffer = new TCByteBufferImpl(size, true);
        refs.add(new SoftReference<>(buffer, gcqueue));
      }
    } else {
      buffer.unlock();
    }
    return buffer;
  }

  public int referenced() {
    return refs.size();
  }

  @Override
  public TCByteBuffer peek() {
    throw new UnsupportedOperationException();
  }

  public void close() {
    if (parent != null) {
      localpool.forEach(parent::offer);
    }
  }
}
