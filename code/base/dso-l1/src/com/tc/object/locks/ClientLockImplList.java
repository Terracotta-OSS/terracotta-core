/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.object.locks.LockStateNode.LockHold;
import com.tc.object.locks.LockStateNode.LockWaiter;
import com.tc.object.locks.LockStateNode.PendingLockHold;
import com.tc.util.SinglyLinkedList;
import com.tc.util.SynchronizedSinglyLinkedList;

public class ClientLockImplList extends SynchronizedSinglyLinkedList<LockStateNode> {

  private static final Filter<LockStateNode> HOLDS = new Filter<LockStateNode>() {
    public boolean accept(LockStateNode object) {
      return object instanceof LockHold;
    }

    public boolean terminate(LockStateNode object) {
      return object instanceof LockWaiter || object instanceof PendingLockHold;
    }    
  };
  
  private static final Filter<LockStateNode> PENDING_HOLDS = new Filter<LockStateNode>() {
    public boolean accept(LockStateNode object) {
      return object instanceof PendingLockHold;
    }

    public boolean terminate(LockStateNode object) {
      return false;
    }
  };
  
  private static final Filter<LockStateNode> WAITERS = new Filter<LockStateNode>() {
    public boolean accept(LockStateNode object) {
      return object instanceof LockWaiter;
    }

    public boolean terminate(LockStateNode object) {
      return false;
    }
  };
  
  private static final Filter<LockStateNode> HOLDS_AND_WAITERS = new Filter<LockStateNode>() {
    public boolean accept(LockStateNode object) {
      return object instanceof LockHold || object instanceof LockWaiter;
    }

    public boolean terminate(LockStateNode object) {
      return false;
    }
  };
  
  public FilteredIterator<LockHold> holds() {
    return new FilteredIterator(HOLDS);
  }

  public FilteredIterator<PendingLockHold> pendingHolds() {
    return new FilteredIterator<PendingLockHold>(PENDING_HOLDS);
  }
  
  public FilteredIterator<LockWaiter> waiters() {
    return new FilteredIterator<LockWaiter>(WAITERS);
  }
  
  public FilteredIterator<LockStateNode> holdsAndWaiters() {
    return new FilteredIterator<LockStateNode>(HOLDS_AND_WAITERS);
  }
  
  class FilteredIterator<T extends LockStateNode> extends SinglyLinkedList<LockStateNode>.ListIterator {

    private final Filter<LockStateNode> filter;

    private T                           next;
    
    public FilteredIterator(Filter<LockStateNode> filter) {
      this.filter = filter;
      next = getNext();
    }
    
    @Override
    public boolean hasNext() {
      return next != null;
    }

    @Override
    public T next() {
      T current = next;
      next = getNext();
      return current;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
    
    private T getNext() {
      while (super.hasNext()) {
        LockStateNode o = super.next();
        if (filter.accept(o)) {
          return (T) o;
        } else if (filter.terminate(o)) {
          return null;
        }
      }
      return null;
    }
  }
  
  interface Filter<T> {    
    public boolean accept(T object);
    public boolean terminate(T object);
  }
}
