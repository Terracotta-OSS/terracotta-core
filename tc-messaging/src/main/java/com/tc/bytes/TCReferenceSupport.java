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

import com.tc.util.Assert;
import com.tc.util.concurrent.SetOnceFlag;
import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class TCReferenceSupport {
  private final Consumer<TCByteBuffer> returns;
  private final Collection<TCByteBuffer> items;
  private final AtomicInteger referenceCount = new AtomicInteger();
  
  // Ideally the below variables are not needed.  These are here to track references
  // that are created but not closed, holding resources.
  private final MemoryTracker track;
  
  private static final Logger LOGGER = LoggerFactory.getLogger(TCReferenceSupport.class);
  private static final Set<TCReferenceSupport> COMMITTED_REFERENCES = ConcurrentHashMap.newKeySet();
  private static volatile boolean TRACK_REFERENCES;

  private TCReferenceSupport(Collection<TCByteBuffer> tracked, Consumer<TCByteBuffer> returns) {
    this.items = tracked;
    this.returns = returns;
    if (TRACK_REFERENCES) {
      track = new MemoryTracker();
    } else {
      track = null;
    }
  }
  
  public static void startMonitoringReferences() {
    TRACK_REFERENCES = true;
  }
  
  public static void stopMonitoringReferences() {
    TRACK_REFERENCES = false;
    COMMITTED_REFERENCES.clear();
  }
  
  public static int checkReferences() {
    return COMMITTED_REFERENCES.stream().map(TCReferenceSupport::gc).reduce(0, Integer::sum);
  }
  /**
   * General reference counter.  Starts with a reference count of one.  Each duplicate reference 
   * increases the reference count by one.  Every close decreases by one.  Once all the references
   * are closed, the byte buffers are recycled back to the return queue.
   * 
   * @param tracked
   * @param returns
   * @return 
   */
  public static TCReference createReference(Collection<TCByteBuffer> tracked, Consumer<TCByteBuffer> returns) {
    if (returns == null) {
      returns = c->{};
    }
    return new TCReferenceSupport(tracked, returns).reference();
  }
  
  public static TCReference createReference(Consumer<TCByteBuffer> returns, TCByteBuffer...tracked) {
    if (returns == null) {
      returns = c->{};
    }
    return new TCReferenceSupport(Arrays.asList(tracked), returns).reference();
  }
  
  public static TCReference createDirectReference(Collection<TCByteBuffer> tracked, Consumer<TCByteBuffer> returns) {
    if (returns == null) {
      returns = c->{};
    }
    return new TCReferenceSupport(tracked, returns).directReference();
  }
  
  private void reclaim() {
    Assert.assertTrue(referenceCount.get() == 0);
    for (TCByteBuffer buf : items) {
      returns.accept(buf.reInit());
    }
    COMMITTED_REFERENCES.remove(this);
  }

  private int gc() {
    if (track != null) {
      return track.gc();
    } else {
      return 0;
    }
  }
  /**
   * Helper to wrap byte buffers that don't need recycling or are recycled by other reference counting
   * 
   * @param tracked
   * @return 
   */
  public static TCReference createGCReference(Collection<TCByteBuffer> tracked) {
    return new GCRef(tracked);
  }
  
  public static TCReference createGCReference(TCByteBuffer...tracked) {
    return createGCReference(Arrays.asList(tracked));
  }
  /**
   * An aggregate reference takes a duplicate reference of each item in the collection 
   * and manages them as a single reference.  The original references plus the aggregate reference must be closed 
   * in order for the byte buffers to be reclaimed.
   * @param tracked
   * @return 
   */
  public static TCReference createAggregateReference(Collection<TCReference> tracked) {
    return new RefRef(tracked);
  }
  
  public static TCReference createAggregateReference(TCReference...tracked) {
    return createAggregateReference(Arrays.asList(tracked));
  }
  
  private Ref reference() {
    if (track != null) {
      COMMITTED_REFERENCES.add(this);
    }
    return new Ref(items, TCByteBuffer::asReadOnlyBuffer);
  }
  
  private DirectRef directReference() {
    if (track != null) {
      COMMITTED_REFERENCES.add(this);
    }
    return new DirectRef();
  }
  
  private static class GCRef implements TCReference {
    private final List<TCByteBuffer> buffers;

    public GCRef(Collection<TCByteBuffer> buffers) {
      this.buffers = buffers.stream().filter(TCByteBuffer::hasRemaining).map(TCByteBuffer::slice).collect(Collectors.toList());
    }

    @Override
    public TCReference duplicate() {
      return new GCRef(buffers);
    }

    @Override
    public void close() {
      
    }

    @Override
    public Iterator<TCByteBuffer> iterator() {
      return buffers.iterator();
    }
  
  }
  
  private static class RefRef implements TCReference {
    private final List<TCReference> localItems;
    
    RefRef(Collection<TCReference> run) {
      this.localItems = run.stream().map(TCReference::duplicate).collect(toUnmodifiableList());
    }

    @Override
    public TCReference duplicate() {
      return new RefRef(localItems);
    }

    @Override
    public void close() {
      this.localItems.forEach(TCReference::close);
    }

    @Override
    public Iterator<TCByteBuffer> iterator() {
      return this.localItems.stream().flatMap(r->StreamSupport.stream(r.spliterator(), false)).iterator();
    }
  }
  
  private class DirectRef implements TCReference {
    private final SetOnceFlag closed = new SetOnceFlag();
    private final Reference<TCReference> tracker = track == null ? null : track.startTracking(this);

    @Override
    public TCReference duplicate() {
      return new DirectRef();
    }

  
    @Override
    public void close() {
      if (closed.attemptSet()) {
        if (track != null) {
          track.stopTracking(tracker);
        }
        if (referenceCount.decrementAndGet() == 0) {
          reclaim();
        }
      }
    }

    @Override
    public Iterator<TCByteBuffer> iterator() {
      if (closed.isSet()) {
        throw new IllegalStateException("reference is closed");
      }
      return items.iterator();
    }
  }
  
  private class Ref implements TCReference {
    
    private final Collection<TCByteBuffer> localItems;
    private final Reference<TCReference> tracker;
    private final SetOnceFlag closed = new SetOnceFlag();
    
    Ref(Collection<TCByteBuffer> localItems, Function<TCByteBuffer, TCByteBuffer> mapper) {
      referenceCount.getAndIncrement();
      this.tracker = track == null ? null : track.startTracking(this);
      this.localItems = localItems.stream().map(mapper).filter(TCByteBuffer::hasRemaining).collect(toUnmodifiableList());
    }
  
    @Override
    public void close() {
      if (closed.attemptSet()) {
        if (track != null) {
          track.stopTracking(tracker);
        }
        if (referenceCount.decrementAndGet() == 0) {
          reclaim();
        }
      }
    }

    @Override
    public Iterator<TCByteBuffer> iterator() {
      checkClosed();
      return localItems.iterator();
    }
    
    private void checkClosed() {
      if (closed.isSet()) {
        throw new IllegalStateException("reference is closed");
      }
    }
  
    @Override
    public Ref duplicate() {
      checkClosed();
      return new Ref(localItems, TCByteBuffer::slice);
    }
  }
  
  private class MemoryTracker {
    private final Map<Reference<? extends TCReference>, Exception> outRefs = new ConcurrentHashMap<>();
    private final ReferenceQueue<TCReference> gcRefs = new ReferenceQueue<>();
    
    private Reference<TCReference> startTracking(TCReference ref) {
      Reference<TCReference> tracker = new PhantomReference<>(ref, gcRefs);
      Assert.assertNull(outRefs.put(tracker, new Exception()));
      return tracker;
    }
    
    private void stopTracking(Reference<TCReference> ref) {
      Assert.assertNotNull(outRefs.remove(ref));
    }

    private int gc() {
      Reference<? extends TCReference> next = gcRefs.poll();
      int count = 0;
      while (next != null) {
        Exception stack = outRefs.remove(next);
        Assert.assertNotNull(stack);
        LOGGER.warn("memory reference found that was not properly closed. ", stack);
        if (referenceCount.decrementAndGet() == 0) {
          reclaim();
        }
        next = gcRefs.poll();
        count += 1;
      }
      return count;
    }
  }
  
  private static <T> Collector<? super T, ?, List<T>> toUnmodifiableList() {
    return Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList);
  }
}
