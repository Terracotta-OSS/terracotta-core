/**
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util;

import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.Iterator;
import java.util.List;

/*
 * This stack implementation uses ArrayList internally. This is mainly created so that we dont have synchronization
 * overheads that java.util.Stack imposses since it is based on Vector. This class maintains an interface level compatibility
 * with java.util.Stack but doesnot implement all of Vector interfaces.
 */
public class Stack {

  private final List list = new ArrayList();

  /**
   * Creates an empty Stack.
   */
  public Stack() {
  }

  /**
   * Pushes an item onto the top of this stack. This has exactly the same effect as: <blockquote>
   * 
   * @param item the item to be pushed onto this stack.
   * @return the <code>item</code> argument.
   * @see java.util.Vector#addElement
   */
  public Object push(Object item) {
    list.add(item);
    return item;
  }

  /**
   * Removes the object at the top of this stack and returns that object as the value of this function.
   * 
   * @return The object at the top of this stack (the last item of the <tt>Vector</tt> object).
   * @exception EmptyStackException if this stack is empty.
   */
  public Object pop() {
    int len = size();

    if (len == 0) throw new EmptyStackException();
    return list.remove(len - 1);
  }

  /**
   * Looks at the object at the top of this stack without removing it from the stack.
   * 
   * @return the object at the top of this stack (the last item of the <tt>Vector</tt> object).
   * @exception EmptyStackException if this stack is empty.
   */
  public Object peek() {
    int len = size();

    if (len == 0) throw new EmptyStackException();
    return list.get(len - 1);
  }

  /**
   * Tests if this stack is empty.
   * 
   * @return <code>true</code> if and only if this stack contains no items; <code>false</code> otherwise.
   */
  public boolean empty() {
    return size() == 0;
  }
  
  /**
   * Size of this Stack
   * 
   * @return the size of the stack
   */
  public int size() {
    return list.size();
  }

  /**
   * Returns the 1-based position where an object is on this stack. If the object <tt>o</tt> occurs as an item in this
   * stack, this method returns the distance from the top of the stack of the occurrence nearest the top of the stack;
   * the topmost item on the stack is considered to be at distance <tt>1</tt>. The <tt>equals</tt> method is used
   * to compare <tt>o</tt> to the items in this stack.
   * 
   * @param o the desired object.
   * @return the 1-based position from the top of the stack where the object is located; the return value
   *         <code>-1</code> indicates that the object is not on the stack.
   */
  public int search(Object o) {
    int i = list.lastIndexOf(o);

    if (i >= 0) { return size() - i; }
    return -1;
  }

  private static final long serialVersionUID = 343422342343423234L;

  
  /* I am not in big favor of having these interfaces */
  
  public Object get(int index) {
    return list.get(index);
  }

  public Object remove(int index) {
    return list.remove(index);
  }

  public Iterator iterator() {
    return list.iterator();
  }

  public boolean isEmpty() {
    return empty();
  }

  public boolean contains(Object o) {
    return list.contains(o);
  }
  
  public boolean remove(Object o) {
    return list.remove(o);
  }
}
