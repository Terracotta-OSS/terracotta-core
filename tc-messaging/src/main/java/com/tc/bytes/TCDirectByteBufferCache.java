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

import com.tc.util.concurrent.SetOnceFlag;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.AbstractQueue;
import java.util.Collections;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Stream;

/**
 *
 */
public class TCDirectByteBufferCache extends AbstractQueue<TCByteBuffer> {
  private final ReferenceQueue<TCByteBuffer> gcqueue = new ReferenceQueue<>();
  private final Set<Reference<? extends TCByteBuffer>> refs = ConcurrentHashMap.newKeySet();
  private final Queue<TCByteBuffer> parent;

  private final int size;
  private final Queue<TCByteBuffer> localpool;
  private final SetOnceFlag closed = new SetOnceFlag();

  public TCDirectByteBufferCache() {
    this(TCByteBufferFactory.getFixedBufferSize());
  }

  public TCDirectByteBufferCache(int size) {
    this(new NullQueue(), size, Integer.MAX_VALUE);
  }

  public TCDirectByteBufferCache(int size, int limit) {
    this(new NullQueue(), size, limit);
  }

  public TCDirectByteBufferCache(Queue<TCByteBuffer> parent) {
    this(parent, TCByteBufferFactory.getFixedBufferSize(), Integer.MAX_VALUE);
  }
  
  private TCDirectByteBufferCache(Queue<TCByteBuffer> parent, int size, int limit) {
    this.parent = parent;
    this.size = size;
    this.localpool = new LinkedBlockingQueue<>(limit);
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
    return Stream.concat(localpool.stream(), parent.stream()).iterator();
  }

  @Override
  public int size() {
    return localpool.size() + parent.size();
  }

  @Override
  public boolean offer(TCByteBuffer e) {
    if (e instanceof TCByteBufferImpl) {
      ((TCByteBufferImpl)e).verifyLocked();
    }
    return (closed.isSet() || !localpool.offer(e)) ? parent.offer(e) : true;
  }

  @Override
  public TCByteBuffer poll() {
    processReferencePool();
    if (closed.isSet()) {
      return null;
    }
    TCByteBuffer buffer = localpool.poll();
    if (buffer == null) {
      buffer = parent.poll();
      if (buffer == null) {
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
    return localpool.peek();
  }

  @Override
  public void clear() {
    localpool.clear();
  }

  public void close() {
    if (closed.attemptSet()) {
      while (!localpool.isEmpty()) {
        parent.offer(localpool.remove());
      }
    }
  }

  private static class NullQueue extends AbstractQueue<TCByteBuffer> {

    @Override
    public Iterator<TCByteBuffer> iterator() {
      return Collections.emptyIterator();
    }

    @Override
    public int size() {
      return 0;
    }

    @Override
    public boolean offer(TCByteBuffer e) {
      return false;
    }

    @Override
    public TCByteBuffer poll() {
      return null;
    }

    @Override
    public TCByteBuffer peek() {
      return null;
    }
    
  }
}
