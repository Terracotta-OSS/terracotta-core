/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util.concurrent;

import java.util.NoSuchElementException;

/**
 * A generic queue interface similar to the java.util.Queue interface in the JDK 1.5 API NOTE: the interface is not
 * complete with respect to the JDK version. Feel free to complete it if you'd like
 * 
 * @author orion
 */
public interface Queue {
  /**
   * @return true iff this queue is empty (ie. contains no elements)
   */
  boolean isEmpty();

  /**
   * Retrieves and removes the head of this queue, or null if this queue is empty.
   * 
   * @return the head of this queue, or null if this queue is empty.
   */
  Object element();

  /**
   * Inserts the specified element into this queue, if possible
   * 
   * @param o the element to insert.
   * @return true if it was possible to add the element to this queue, else false
   */
  boolean offer(Object o);

  /**
   * Retrieves, but does not remove, the head of this queue, returning null if this queue is empty.
   * 
   * @return the head of this queue, or null if this queue is empty.
   */
  Object peek();

  /**
   * Retrieves and removes the head of this queue, or null if this queue is empty.
   * 
   * @return the head of this queue, or null if this queue is empty.
   */
  Object poll();

  /**
   * Retrieves and removes the head of this queue. This method differs from the poll method in that it throws an
   * exception if this queue is empty.
   * 
   * @return the head of this queue.
   * @throws NoSuchElementException if this queue is empty.
   */
  Object remove();
}