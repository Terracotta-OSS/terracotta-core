/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.cache;

import com.tc.util.concurrent.TCConcurrentStore;
import com.tc.util.concurrent.TCConcurrentStore.TCConcurrentStoreCallback;

import java.util.Map;
import java.util.Set;

public class CachedItemStore<L> {

  private final TCConcurrentStore<L, CachedItem> store;
  private final AddCallBack                      addCallback    = new AddCallBack();
  private final RemoveCallBack                   removeCallback = new RemoveCallBack();

  /**
   * Creates a CachedItemStore with the specified initial capacity, load factor and concurrency level.
   */
  public CachedItemStore(int initialCapacity, final float loadFactor, int concurrencyLevel) {
    this.store = new TCConcurrentStore<L, CachedItem>(initialCapacity, loadFactor, concurrencyLevel);
  }

  // For tests
  CachedItem get(final L id) {
    return this.store.get(id);
  }

  public void add(final L id, final CachedItem item) {
    this.store.executeUnderWriteLock(id, item, this.addCallback);
  }

  public void remove(final L id, final CachedItem item) {
    this.store.executeUnderWriteLock(id, item, this.removeCallback);
  }

  public void flush(final L id) {
    final CachedItem head = this.store.remove(id);
    dispose(head);
  }

  private void dispose(CachedItem head) {
    while (head != null) {
      head.dispose();
      head = head.getNext();
    }
  }

  public Set addAllKeysTo(Set keySet) {
    return this.store.addAllKeysTo(keySet);
  }

  private static final class AddCallBack<L> implements TCConcurrentStoreCallback<L, CachedItem> {
    // Called under segment lock
    public Object callback(L id, Object param, Map<L, CachedItem> segment) {
      final CachedItem item = (CachedItem) param;
      final CachedItem old = segment.put(id, item);
      if (old != null) {
        item.setNext(old);
      }
      return true;
    }

  }

  private static final class RemoveCallBack<L> implements TCConcurrentStoreCallback<L, CachedItem> {
    // Called under segment lock
    public Object callback(L id, Object param, Map<L, CachedItem> segment) {
      final CachedItem item = (CachedItem) param;
      CachedItem head = segment.remove(id);
      head = removeNode(head, item);
      if (head != null) {
        segment.put(id, head);
      }
      return true;
    }

    private CachedItem removeNode(CachedItem head, final CachedItem item) {
      if (head == item) {
        // first item is the item of interest
        head = head.getNext();
        item.setNext(null); // Aid GC
      } else if (head != null) {
        CachedItem current = head;
        CachedItem next;
        while ((next = current.getNext()) != null) {
          if (next == item) {
            current.setNext(next.getNext());
            item.setNext(null); // Aid GC
            break; // hopefully only one occurrence
          }
          current = next;
        }
      }
      return head;
    }
  }
}
