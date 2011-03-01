/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.cache;

import com.tc.cache.ExpirableEntry;
import com.tc.object.ObjectID;
import com.tc.object.tx.TransactionCompleteListener;
import com.tc.object.tx.TransactionID;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

public class CachedItem implements TransactionCompleteListener {

  public interface DisposeListener {
    public void disposed(CachedItem ci);
  }

  private final DisposeListener                                                 listener;
  /*
   * Could be a LockID or an ObjectID that this CacheItem is mapped to in RemoteServerMapManagerImpl or it could be null
   * for incoherent items
   */
  private final Object                                                          id;
  private final Object                                                          key;
  private final Object                                                          value;

  private volatile CachedItemState                                              state;
  private static final AtomicReferenceFieldUpdater<CachedItem, CachedItemState> refUpdater = AtomicReferenceFieldUpdater
                                                                                               .newUpdater(CachedItem.class,
                                                                                                           CachedItemState.class,
                                                                                                           "state");

  private volatile boolean                                                      expired    = false;
  private CachedItem                                                            next;

  public CachedItem(final Object id, final DisposeListener listener, final Object key, final Object value,
                    boolean waitForAck) {
    this.listener = listener;
    this.id = id;
    this.key = key;
    this.value = value;
    this.state = (waitForAck ? CachedItemState.UNACKED_ACCESSED : CachedItemState.ACKED_ACCESSED);
  }

  public Object getID() {
    return this.id;
  }

  public Object getKey() {
    return this.key;
  }

  public ExpirableEntry getExpirableEntry() {
    if (this.value instanceof ExpirableEntry) { return (ExpirableEntry) this.value; }
    return null;
  }

  public Object getValue() {
    accessed();
    return this.value;
  }

  public void dispose() {
    this.listener.disposed(this);
  }

  public CachedItem getNext() {
    return this.next;
  }

  public void setNext(final CachedItem next) {
    this.next = next;
  }

  public boolean getAndClearAccessed() {
    final boolean isAccessed = state.isAccessed();
    clearAccessed();
    return isAccessed;
  }

  public boolean isExpired() {
    return this.expired;
  }

  public void markExpired() {
    this.expired = true;
  }

  public DisposeListener getListener() {
    return listener;
  }

  private void accessed() {
    boolean success = false;
    do {
      final CachedItemState lstate = state;
      success = update(lstate, lstate.accessed());
    } while (!success);
  }

  private void clearAccessed() {
    boolean success = false;
    do {
      final CachedItemState lstate = state;
      success = update(lstate, lstate.clearAccessed());
    } while (!success);
  }

  public void transactionComplete(TransactionID txnID) {
    boolean success = false;
    do {
      final CachedItemState lstate = state;
      success = update(lstate, lstate.acknowledged());
    } while (!success);
    if (id == ObjectID.NULL_ID && value == null) {
      // This is an unlocked remove item, remove from local cache so reads go to the server
      dispose();
    }
  }

  private boolean update(CachedItemState previous, CachedItemState newState) {
    return refUpdater.compareAndSet(this, previous, newState);
  }

}