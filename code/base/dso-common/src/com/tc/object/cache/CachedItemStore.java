/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.cache;

import com.tc.object.locks.LockID;
import com.tc.util.concurrent.TCConcurrentStore;
import com.tc.util.concurrent.TCConcurrentStore.TCConcurrentStoreCallback;

import java.util.Map;

public class CachedItemStore {

  private final TCConcurrentStore<LockID, CachedItem> store;
  private final AddCallBack                           addCallback    = new AddCallBack();
  private final RemoveCallBack                        removeCallback = new RemoveCallBack();

  /**
   * Creates a CachedItemStore with the specified initial capacity, load factor and concurrency level.
   */
  public CachedItemStore(int initialCapacity, final float loadFactor, int concurrencyLevel) {
    this.store = new TCConcurrentStore<LockID, CachedItem>(initialCapacity, loadFactor, concurrencyLevel);
  }

  // For tests
  CachedItem get(final LockID lockID) {
    return this.store.get(lockID);
  }

  public void add(final LockID lockID, final CachedItem item) {
    this.store.executeUnderWriteLock(lockID, item, this.addCallback);
  }

  public void remove(final LockID lockID, final CachedItem item) {
    this.store.executeUnderWriteLock(lockID, item, this.removeCallback);
  }

  public void flush(final LockID lockID) {
    final CachedItem head = this.store.remove(lockID);
    dispose(head);
  }

  private void dispose(CachedItem head) {
    while (head != null) {
      head.dispose();
      head = head.getNext();
    }
  }

  private static final class AddCallBack implements TCConcurrentStoreCallback<LockID, CachedItem> {
    // Called under segment lock
    public Object callback(LockID lockID, Object param, Map<LockID, CachedItem> segment) {
      final CachedItem item = (CachedItem) param;
      final CachedItem old = segment.put(lockID, item);
      if (old != null) {
        item.setNext(old);
      }
      return true;
    }

  }

  private static final class RemoveCallBack implements TCConcurrentStoreCallback<LockID, CachedItem> {
    // Called under segment lock
    public Object callback(LockID lockID, Object param, Map<LockID, CachedItem> segment) {
      final CachedItem item = (CachedItem) param;
      CachedItem head = segment.remove(lockID);
      head = removeNode(head, item);
      if (head != null) {
        segment.put(lockID, head);
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
