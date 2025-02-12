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

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * <p>
 * This is a Singly-Linked list implementation, with head and tail pointers.
 * <p>
 * This is written mainly for saving space. So a Singly-Linked saves space by not having the previous pointer in each
 * element, compared to java.util.LinkedList. Also to save space, extra objects are not created to store the elements.
 * This means that the elements should implement the LinkedNode Interface.
 * <p>
 * Also to save further space, one can extend this object instead of storing a reference to an instance of this object.
 * Of course that is not possible in all cases.
 * <p>
 * Most common operations like add/remove/get first element, last element and some element in the middle are all
 * supported. An Iterator is implemented to facilitate adding in the middle and iterating all the elements. The Iterator
 * of this class does not throw ConcurrentModificationException as we don't want to store mod count to save space.
 */
public class SinglyLinkedList<E extends SinglyLinkedList.LinkedNode<E>> implements Iterable<E> {

  public interface LinkedNode<L extends LinkedNode<L>> {

    public L getNext();

    public L setNext(L next);
  }

  public interface SinglyLinkedListIterator<I extends LinkedNode<I>> extends Iterator<I> {

    @Override
    public boolean hasNext();

    @Override
    public I next();

    // Removes the last returned element from the list
    @Override
    public void remove();

    // Adds the element e next to the last returned element, if its not removed. Else throws IllegalStateException.
    public void addNext(I e);

    // Adds the element e previous to the last returned element, if its not removed. Else throws IllegalStateException.
    public void addPrevious(I e);
  }

  private E head;
  private E tail;

  public SinglyLinkedList() {
    this.head = this.tail = null;
  }

  public boolean isEmpty() {
    return this.head == null;
  }

  public void addFirst(E first) {
    first.setNext(this.head);
    this.head = first;
    if (this.tail == null) {
      this.tail = first;
    }
  }

  public E removeFirst() {
    if (this.head == null) { throw new NoSuchElementException(); }
    final E first = this.head;
    this.head = first.setNext(null);
    if (this.tail == first) {
      this.tail = null;
    }
    return first;
  }

  public E getFirst() {
    if (this.head == null) { throw new NoSuchElementException(); }
    return this.head;
  }

  public void addLast(E last) {
    if (this.tail == null) {
      addFirst(last);
      return;
    }
    this.tail.setNext(last);
    this.tail = last;
  }

  /**
   * This is costly compared to the other methods as you have to iterate to the end.
   */
  public E removeLast() {
    if (this.tail == null) { throw new NoSuchElementException(); }
    if (this.head == this.tail) { return removeFirst(); }
    E cur = this.head;
    E next;
    while ((next = cur.getNext()) != this.tail) {
      cur = next;
    }
    cur.setNext(null);
    this.tail = cur;
    return next;
  }

  public E getLast() {
    if (this.tail == null) { throw new NoSuchElementException(); }
    return this.tail;
  }

  public E remove(E obj) {
    E current = null;
    E next = this.head;

    while (next != null) {
      final E prev = current;
      current = next;
      next = next.getNext();

      if (current == obj || obj.equals(current)) {
        // remove
        if (prev == null) {
          this.head = current.setNext(null);
        } else {
          prev.setNext(current.setNext(null));
        }
        if (next == null) {
          this.tail = prev;
        }

        return current;
      }
    }

    return null;
  }

  @Override
  public SinglyLinkedListIterator<E> iterator() {
    return new ListIterator();
  }

  /**
   * This implementation does not throw ConcurrentModification Exception as we don't want to store mod count to save
   * space.
   */
  protected class ListIterator implements SinglyLinkedListIterator<E> {

    E prev;
    E current;
    E next;

    protected ListIterator() {
      this.prev = this.current = null;
      this.next = SinglyLinkedList.this.head;
    }

    @Override
    public boolean hasNext() {
      return this.next != null;
    }

    @Override
    public E next() {
      if (this.next == null) { throw new NoSuchElementException(); }
      this.prev = this.current;
      this.current = this.next;
      this.next = this.next.getNext();
      return this.current;
    }

    @Override
    public void addNext(E e) {
      if (this.current == null || this.current == this.prev) { throw new IllegalStateException(); }
      e.setNext(this.next);
      this.current.setNext(e);
      if (this.next == null) {
        SinglyLinkedList.this.tail = e;
      }
      this.next = e;
    }

    @Override
    public void addPrevious(E e) {
      if (this.current == null || this.current == this.prev) { throw new IllegalStateException(); }
      e.setNext(this.current);
      if (this.prev == null) {
        SinglyLinkedList.this.head = e;
      } else {
        this.prev.setNext(e);
      }
      this.prev = e;
    }

    @Override
    public void remove() {
      if (this.current == null) { throw new IllegalStateException(); }
      if (this.prev == null) {
        SinglyLinkedList.this.head = this.current.setNext(null);
      } else {
        this.prev.setNext(this.current.setNext(null));
      }
      if (this.next == null) {
        SinglyLinkedList.this.tail = this.prev;
      }
      this.current = this.prev;
    }

  }
}
