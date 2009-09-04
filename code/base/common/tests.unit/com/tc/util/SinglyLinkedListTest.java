/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util;

import com.tc.util.SinglyLinkedList.LinkedNode;
import com.tc.util.SinglyLinkedList.SinglyLinkedListIterator;

import junit.framework.TestCase;

public class SinglyLinkedListTest extends TestCase {

  public void testBasic() {
    SinglyLinkedList<Node> list = new SinglyLinkedList<Node>();
    list.addFirst(new Node(3));
    list.addFirst(new Node(1));
    list.addLast(new Node(5));

    SinglyLinkedListIterator<Node> i = list.iterator();
    assertTrue(i.hasNext());
    assertEquals(new Node(1), i.next());
    assertTrue(i.hasNext());
    assertEquals(new Node(3), i.next());
    assertTrue(i.hasNext());
    assertEquals(new Node(5), i.next());
    assertFalse(i.hasNext());

    i.addNext(new Node(6));
    assertTrue(i.hasNext());
    assertEquals(new Node(6), i.next());

    i = list.iterator();
    assertTrue(i.hasNext());
    assertEquals(new Node(1), i.next());
    i.remove();
    assertTrue(i.hasNext());
    assertEquals(new Node(3), i.next());

    i = list.iterator();
    assertTrue(i.hasNext());
    assertEquals(new Node(3), i.next());
    i.addPrevious(new Node(2));

    i = list.iterator();
    assertTrue(i.hasNext());
    assertEquals(new Node(2), i.next());
    assertTrue(i.hasNext());
    assertEquals(new Node(3), i.next());

    assertEquals(new Node(2), list.getFirst());
    assertEquals(new Node(6), list.getLast());

    assertEquals(new Node(2), list.removeFirst());
    assertEquals(new Node(6), list.removeLast());

    assertFalse(list.isEmpty());

    i = list.iterator();
    assertTrue(i.hasNext());
    assertEquals(new Node(3), i.next());
    i.remove();
    assertTrue(i.hasNext());
    assertEquals(new Node(5), i.next());
    i.addPrevious(new Node(1));
    i.addNext(new Node(2));
    i.remove();

    assertEquals(new Node(1), list.removeFirst());
    assertEquals(new Node(2), list.removeLast());

    assertTrue(list.isEmpty());
  }

  protected class Node implements LinkedNode<Node> {

    private Node      next;
    private final int id;

    public Node(int id) {
      this.id = id;
    }

    public Node getNext() {
      return this.next;
    }

    public Node setNext(Node n) {
      Node old = this.next;
      this.next = n;
      return old;
    }

    @Override
    public int hashCode() {
      return this.id;
    }

    @Override
    public boolean equals(Object o) {
      if (o instanceof Node) {
        Node n = (Node) o;
        return n.id == this.id;
      }
      return false;
    }

    @Override
    public String toString() {
      return "Node(" + this.id + ") ";
    }
  }
}
