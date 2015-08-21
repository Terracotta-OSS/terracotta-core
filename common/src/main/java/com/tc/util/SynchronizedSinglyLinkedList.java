/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util;

/**
 * Synchronized version of SinglyLinkedList
 * <p>
 * This class extends rather than wraps SinglyLinkedList to avoid incurring the memory
 * cost a new object - it is used in the ClientLockImpl low heap usage is critical.
 * <p>
 * Note that the iterator is not synchronized.  You must externally synchronize when
 * using iterators.
 */
public class SynchronizedSinglyLinkedList<E extends SinglyLinkedList.LinkedNode<E>> extends SinglyLinkedList<E> {
  @Override
  public synchronized boolean isEmpty() {
    return super.isEmpty();
  }

  @Override
  public synchronized void addFirst(E first) {
    super.addFirst(first);
  }
  
  @Override
  public synchronized E removeFirst() {
    return super.removeFirst();
  }

  @Override
  public synchronized E getFirst() {
    return super.getFirst();
  }

  @Override
  public synchronized void addLast(E last) {
    super.addLast(last);
  }

  @Override
  public synchronized E removeLast() {
    return super.removeLast();
  }

  
  @Override
  public synchronized E getLast() {
    return super.getLast();
  }

  @Override
  public synchronized E remove(E obj) {
    return super.remove(obj);
  }
}
