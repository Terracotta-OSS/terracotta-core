/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.cache;

import com.tc.object.cache.CachedItem.CachedItemInitialization;
import com.tc.object.cache.CachedItem.DisposeListener;
import com.tc.object.locks.LockID;
import com.tc.object.locks.StringLockID;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import junit.framework.TestCase;

public class CachedItemStoreTest extends TestCase {

  CachedItemStore                       store;
  ConcurrentHashMap<Object, CachedItem> parent;
  HashMap<LockID, Integer>              lockID2Index;
  private DisposeListener               disposeHandler;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    this.store = new CachedItemStore(256, 0.75f, 16);
    this.parent = new ConcurrentHashMap<Object, CachedItem>();
    this.lockID2Index = new HashMap<LockID, Integer>();
    this.disposeHandler = new CachedItem.DisposeListener() {
      public void disposed(final CachedItem ci) {
        CachedItemStoreTest.this.parent.remove(ci.getKey());
      }

      public void evictFromLocalCache(CachedItem ci) {
        CachedItemStoreTest.this.parent.remove(ci.getKey());
      }

    };
  }

  public void test() {
    final ArrayList<CachedItem> lastEntriesOfTwo = new ArrayList<CachedItem>();
    final ArrayList<CachedItem> middleEntriesOfThree = new ArrayList<CachedItem>();
    for (int i = 0; i < 50; i++) {
      final LockID lockID = getLockId(i);
      CachedItem item = new CachedItem(lockID, this.disposeHandler, getKey(i), getValue(i),
                                       CachedItemInitialization.NO_WAIT_FOR_ACK);
      this.parent.put(getKey(i), item);
      this.store.add(lockID, item);
      if (i % 2 == 0) {
        lastEntriesOfTwo.add(item);
        item = new CachedItem(lockID, this.disposeHandler, getKey(i + 10000), getValue(i + 10000),
                              CachedItemInitialization.NO_WAIT_FOR_ACK);
        this.parent.put(getKey(i + 10000), item);
        this.store.add(lockID, item);
      } else if (i % 3 == 0) {
        item = new CachedItem(lockID, this.disposeHandler, getKey(i + 20000), getValue(i + 20000),
                              CachedItemInitialization.NO_WAIT_FOR_ACK);
        middleEntriesOfThree.add(item);
        this.parent.put(getKey(i + 20000), item);
        this.store.add(lockID, item);
        item = new CachedItem(lockID, this.disposeHandler, getKey(i + 30000), getValue(i + 30000),
                              CachedItemInitialization.NO_WAIT_FOR_ACK);
        this.parent.put(getKey(i + 30000), item);
        this.store.add(lockID, item);
      }
    }

    // verify
    for (int i = 0; i < 50; i++) {
      final LockID lockID = getLockId(i);
      final CachedItem item = this.store.get(lockID);
      if (i % 2 == 0) {
        verifyTwo(item, i + 10000, i, lockID);
      } else if (i % 3 == 0) {
        verifyThree(item, i + 30000, i + 20000, i, lockID);
      } else {
        verify(i, lockID, item);
        assertNull(item.getNext());
      }
    }

    // remove last entries
    for (final CachedItem item : lastEntriesOfTwo) {
      final LockID lockID = (LockID) item.getID();
      CachedItem head = this.store.get(lockID);
      final int index = getIndex(lockID);
      verifyTwo(head, index + 10000, index, lockID);
      this.store.remove(lockID, item);
      head = this.store.get(item.getID());
      verifyOne(head, index + 10000, lockID);
    }

    // remove middle entries
    for (final CachedItem item : middleEntriesOfThree) {
      final LockID lockID = (LockID) item.getID();
      CachedItem head = this.store.get(lockID);
      final int index = getIndex(lockID);
      verifyThree(head, index + 30000, index + 20000, index, lockID);
      this.store.remove(lockID, item);
      head = this.store.get(item.getID());
      verifyTwo(head, index + 30000, index, lockID);
    }

    for (int i = 0; i < 50; i++) {
      final LockID lockID = getLockId(i);
      CachedItem item = this.store.get(lockID);
      assertNotNull(item);
      this.store.flush(lockID);
      assertNull(this.store.get(lockID));
      while (item != null) {
        assertNull(this.parent.get(item.getKey()));
        item = item.getNext();
      }
    }
  }

  private int getIndex(final LockID lockID) {
    return this.lockID2Index.get(lockID);
  }

  private void verifyOne(final CachedItem item, final int index, final LockID lockID) {
    assertNull(item.getNext());
    verify(index, lockID, item);
  }

  private void verifyTwo(CachedItem item, final int index1, final int index2, final LockID lockID) {
    verify(index1, lockID, item);
    assertNotNull(item.getNext());
    item = item.getNext();
    verify(index2, lockID, item);
    assertNull(item.getNext());
  }

  private void verifyThree(CachedItem item, final int index1, final int index2, final int index3, final LockID lockID) {
    verify(index1, lockID, item);
    assertNotNull(item.getNext());
    item = item.getNext();
    verify(index2, lockID, item);
    assertNotNull(item.getNext());
    item = item.getNext();
    verify(index3, lockID, item);
    assertNull(item.getNext());
  }

  private void verify(final int i, final LockID lockID, final CachedItem item) {
    assertEquals(lockID, item.getID());
    assertEquals(getKey(i), item.getKey());
    assertEquals(getValue(i), item.getValue());
  }

  private Object getValue(final int i) {
    return "value-" + i;
  }

  private Object getKey(final int i) {
    return "key-" + i;
  }

  private LockID getLockId(final int i) {
    final LockID lockID = new StringLockID("LockID-" + i);
    if (!this.lockID2Index.containsKey(lockID)) {
      this.lockID2Index.put(lockID, i);
    }
    return lockID;
  }

}
