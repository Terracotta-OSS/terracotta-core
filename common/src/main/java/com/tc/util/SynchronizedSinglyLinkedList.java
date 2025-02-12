/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
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
