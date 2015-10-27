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
package com.tc.util;

import java.util.Collection;
import java.util.NoSuchElementException;

/**
 * This singly linked list was written for performance reason. In addition to being a regular linked list, it has the
 * ability to merge other linked lists into this list without traversing through the entire list. <br>
 * <p>
 * Only the basic necessary methods are implemented as of now. But I can see this list implement the List interface
 * someday.
 */
public class MergableLinkedList<T> {
  private int                     size = 0;
  private MergableLinkedList.Node<T> head;
  private MergableLinkedList.Node<T> tail;

  /**
   * Adds the collection to the end of the list(ServerTransaction) txnQ.removeFirst()
   */
  public void addAll(Collection<T> c) {
    for (T element : c) {
      add(element);
    }
  }

  /**
   * This is the reason this class exists. This is an optimized way to add all the elements in List m to the front of
   * this List. At the end of this call the List m is cleared.
   */
  public void mergeToFront(MergableLinkedList<T> m) {
    if (m.isEmpty()) {
      // Nothing to merge
      return;
    }
    m.tail.next = head;
    head = m.head;
    if (tail == null) {
      tail = m.tail;
    }
    size += m.size();
    m.clear();
  }

  public void clear() {
    head = tail = null;
    size = 0;
  }

  public int size() {
    return size;
  }

  public T removeFirst() {
    if (head == null) { throw new NoSuchElementException(); }
    T toReturn = head.data;
    if (head == tail) {
      // Only one element in the list
      head = tail = null;
    } else {
      head = head.next;
    }
    size--;
    return toReturn;
  }

  public boolean isEmpty() {
    return (size == 0);
  }

  /**
   * Adds to the end of the list
   */
  public void add(T t) {
    MergableLinkedList.Node<T> n = new Node<T>(t);
    if (head == null) {
      // first node
      head = n;
    } else {
      tail.next = n;
    }
    tail = n;
    size++;
  }

  private static final class Node<T> {

    private MergableLinkedList.Node<T> next;
    private final T                  data;

    public Node(T data) {
      this.data = data;
    }
  }
}
