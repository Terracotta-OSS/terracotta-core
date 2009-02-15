/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Implements an AA-tree. AA tree provides all the advantages of a Red Black Tree while keeping the implementation
 * simple. For more details on AA tree, check out http://user.it.uu.se/~arnea/abs/simp.html and
 * http://en.wikipedia.org/wiki/AA_tree This source code is taken from
 * http://www.cs.fiu.edu/~weiss/dsaa_java/Code/DataStructures/ and modified slightly.
 * <p>
 * This tree implementation behaves like a set, it doesn't allow duplicate nodes.
 * <p>
 * Note:: "matching" is based on the compareTo method. This class is *NOT* thread safe. Synchronize externally if you
 * want it to be thread safe.
 * 
 * @author Mark Allen Weiss
 */
public class AATreeSet {

  private AANode              root;
  private AANode              deletedNode;
  private AANode              lastNode;
  private Comparable          deletedElement;
  private boolean             inserted;
  private int                 size = 0;

  private static final AANode nullNode;

  static // static initializer for nullNode
  {
    nullNode = new AANode(null);
    nullNode.left = nullNode.right = nullNode;
    nullNode.level = 0;
  }

  /**
   * Construct the tree.
   */
  public AATreeSet() {
    this.root = nullNode;
  }

  public int size() {
    return this.size;
  }

  /**
   * Insert into the tree.
   * 
   * @param x the item to insert.
   * @return true if the item was inserted, false if was already present
   * @throws DuplicateItemException if x is already present.
   */
  public boolean insert(Comparable x) {
    this.inserted = true;
    this.root = insert(x, this.root);
    if (this.inserted) {
      this.size++;
    }
    return this.inserted;
  }

  /**
   * Remove from the tree.
   * 
   * @param x the item to remove.
   * @throws ItemNotFoundException if x is not found.
   */
  public Comparable remove(Comparable x) {
    this.deletedNode = nullNode;
    this.root = remove(x, this.root);
    Comparable d = this.deletedElement;
    // deletedElement is set to null to free the reference,
    // deletedNode is not freed as it will endup pointing to a valid node.
    this.deletedElement = null;
    if (d != null) {
      this.size--;
    }
    return d;
  }

  /**
   * Find the smallest item in the tree.
   * 
   * @return the smallest item or null if empty.
   */
  public Comparable findMin() {
    if (isEmpty()) { return null; }

    AANode ptr = this.root;

    while (ptr.left != nullNode) {
      ptr = ptr.left;
    }

    return ptr.element;
  }

  /**
   * Find the largest item in the tree.
   * 
   * @return the largest item or null if empty.
   */
  public Comparable findMax() {
    if (isEmpty()) { return null; }

    AANode ptr = this.root;

    while (ptr.right != nullNode) {
      ptr = ptr.right;
    }

    return ptr.element;
  }

  /**
   * Find an item in the tree.
   * 
   * @param x the item to search for.
   * @return the matching item of null if not found.
   */

  public Comparable find(Comparable x) {
    AANode current = this.root;

    while (current != nullNode) {
      int res = x.compareTo(current.element);
      if (res < 0) {
        current = current.left;
      } else if (res > 0) {
        current = current.right;
      } else {
        return current.element;
      }
    }
    return null;
  }

  /**
   * Make the tree logically empty.
   */
  public void clear() {
    this.root = nullNode;
    this.size = 0;
  }

  /**
   * Test if the tree is logically empty.
   * 
   * @return true if empty, false otherwise.
   */
  public boolean isEmpty() {
    return this.root == nullNode;
  }

  public Iterator iterator() {
    return new AATreeSetIterator();
  }

  /**
   * Returns an iterator for the tail set greater than or equal to comparable
   */
  public Iterator tailSetIterator(Comparable c) {
    return new AATreeSetIterator(c);
  }

  /**
   * Internal method to insert into a subtree.
   * 
   * @param x the item to insert.
   * @param t the node that roots the tree.
   * @return the new root.
   * @throws DuplicateItemException if x is already present.
   */
  private AANode insert(Comparable x, AANode t) {
    if (t == nullNode) {
      t = new AANode(x);
    } else if (x.compareTo(t.element) < 0) {
      t.left = insert(x, t.left);
    } else if (x.compareTo(t.element) > 0) {
      t.right = insert(x, t.right);
    } else {
      // XXX:: Not throwing DuplicateItemException as we may want to insert elements without doing a lookup.
      // throw new RuntimeException("DuplicateItemExpection:" + x.toString());
      this.inserted = false;
      return t;
    }

    t = skew(t);
    t = split(t);
    return t;
  }

  /**
   * Internal method to remove from a subtree.
   * 
   * @param x the item to remove.
   * @param t the node that roots the tree.
   * @return the new root.
   */
  private AANode remove(Comparable x, AANode t) {
    if (t != nullNode) {
      // Step 1: Search down the tree and set lastNode and deletedNode
      this.lastNode = t;
      if (x.compareTo(t.element) < 0) {
        t.left = remove(x, t.left);
      } else {
        this.deletedNode = t;
        t.right = remove(x, t.right);
      }

      // Step 2: If at the bottom of the tree and
      // x is present, we remove it
      if (t == this.lastNode) {
        if (this.deletedNode == nullNode || x.compareTo(this.deletedNode.element) != 0) {
          // XXX:: Modified to not throw ItemNotFoundException as we want to be able to remove elements without doing a
          // lookup.
          // throw new RuntimeException("ItemNotFoundException : " + x.toString());
        } else {
          this.deletedElement = this.deletedNode.element;
          this.deletedNode.element = t.element;
          t = t.right;
        }
      }

      // Step 3: Otherwise, we are not at the bottom; re-balance
      else if (t.left.level < t.level - 1 || t.right.level < t.level - 1) {
        if (t.right.level > --t.level) {
          t.right.level = t.level;
        }
        t = skew(t);
        t.right = skew(t.right);
        t.right.right = skew(t.right.right);
        t = split(t);
        t.right = split(t.right);
      }
    }
    return t;
  }

  /**
   * Skew primitive for AA-trees.
   * 
   * @param t the node that roots the tree.
   * @return the new root after the rotation.
   */
  private static AANode skew(AANode t) {
    if (t.left.level == t.level) {
      t = rotateWithLeftChild(t);
    }
    return t;
  }

  /**
   * Split primitive for AA-trees.
   * 
   * @param t the node that roots the tree.
   * @return the new root after the rotation.
   */
  private static AANode split(AANode t) {
    if (t.right.right.level == t.level) {
      t = rotateWithRightChild(t);
      t.level++;
    }
    return t;
  }

  /**
   * Rotate binary tree node with left child.
   */
  private static AANode rotateWithLeftChild(AANode k2) {
    AANode k1 = k2.left;
    k2.left = k1.right;
    k1.right = k2;
    return k1;
  }

  /**
   * Rotate binary tree node with right child.
   */
  private static AANode rotateWithRightChild(AANode k1) {
    AANode k2 = k1.right;
    k1.right = k2.left;
    k2.left = k1;
    return k2;
  }

  public String dump() {
    return "AATree = { " + this.root.dump() + " }";
  }

  private static class AANode {
    // Constructors
    AANode(Comparable theElement) {
      this.element = theElement;
      this.left = this.right = nullNode;
      this.level = 1;
    }

    // XXX:: for debugging - costly operation
    public String dump() {
      String ds = String.valueOf(this.element);
      if (this.left != nullNode) {
        ds = ds + "," + this.left.dump();
      }
      if (this.right != nullNode) {
        ds = ds + "," + this.right.dump();
      }
      return ds;
    }

    Comparable element; // The data in the node
    AANode     left;   // Left child
    AANode     right;  // Right child
    int        level;  // Level

    @Override
    public String toString() {
      return "AANode@" + System.identityHashCode(this) + "{" + this.element + "}";
    }
  }

  /*
   * This class is slightly inefficient in that it uses a stack internally to store state. But it is needed so that we
   * don't have to store parent references.
   */
  private class AATreeSetIterator implements Iterator {

    AANode next = AATreeSet.this.root;
    // Contains elements while traversing
    Stack  s    = new Stack();

    public AATreeSetIterator() {
      //
    }

    /**
     * creates an iterator for the tail set greater than or equal to c
     */
    public AATreeSetIterator(Comparable c) {
      int result = 0;
      while (this.next != nullNode) {
        result = c.compareTo(this.next.element);
        if (result < 0) {
          this.s.push(this.next);
          this.next = this.next.left;
        } else if (result == 0) {

          // We are suppose to retain a Tree { elements >= c} . So, put a
          // "take diversion board" in the left subtree. We need to push the next
          // element here, so that iterator.next can pop and start traversing
          // from the right child
          this.s.push(this.next);
          this.next = nullNode;
          break;
        } else if (result > 0) {
          this.next = this.next.right;
        }
      }

      // next (which has already been pushed to the stack) points to Tree Node
      // which is next greater element or null
    }

    public boolean hasNext() {
      if (this.next == nullNode && this.s.size() == 0) { return false; }
      return true;
    }

    public Object next() {
      if (this.next == nullNode && this.s.size() == 0) { throw new NoSuchElementException(); }

      while (true) {
        if (this.next != nullNode) {
          this.s.push(this.next);
          this.next = this.next.left;
        } else {
          AANode current = (AANode) this.s.pop();
          this.next = current.right;
          return current.element;
        }
      }
    }

    // This is a little tricky, the tree might re-balance itself.
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }
}