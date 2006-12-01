/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util;

import com.tc.exception.ImplementMe;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Implements an AA-tree. AA tree provides all the advantages of a Red Black Tree while keeping the implementation
 * simple. For more details on AA tree, check out http://user.it.uu.se/~arnea/abs/simp.html and
 * http://en.wikipedia.org/wiki/AA_tree This source code is taken from
 * http://www.cs.fiu.edu/~weiss/dsaa_java/Code/DataStructures/ and modified slightly. Note:: "matching" is based on the
 * compareTo method. This class is *NOT* thread safe. Synchronize externally if you want it to be thread safe.
 * 
 * @author Mark Allen Weiss
 */
public class AATree {

  private AANode              root;
  private AANode              deletedNode;
  private AANode              lastNode;
  private Comparable          deletedElement;
  private boolean             inserted;

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
  public AATree() {
    root = nullNode;
  }

  /**
   * Insert into the tree.
   * 
   * @param x the item to insert.
   * @return true if the item was inserted, false if was already present
   * @throws DuplicateItemException if x is already present.
   */
  public boolean insert(Comparable x) {
    inserted = true;
    root = insert(x, root);
    return inserted;
  }

  /**
   * Remove from the tree.
   * 
   * @param x the item to remove.
   * @throws ItemNotFoundException if x is not found.
   */
  public Comparable remove(Comparable x) {
    deletedNode = nullNode;
    root = remove(x, root);
    Comparable d = deletedElement;
    // deletedElement is set to null to free the reference,
    // deletedNode is not freed as it will endup pointing to a valid node.
    deletedElement = null;
    return d;
  }

  /**
   * Find the smallest item in the tree.
   * 
   * @return the smallest item or null if empty.
   */
  public Comparable findMin() {
    if (isEmpty()) return null;

    AANode ptr = root;

    while (ptr.left != nullNode)
      ptr = ptr.left;

    return ptr.element;
  }

  /**
   * Find the largest item in the tree.
   * 
   * @return the largest item or null if empty.
   */
  public Comparable findMax() {
    if (isEmpty()) return null;

    AANode ptr = root;

    while (ptr.right != nullNode)
      ptr = ptr.right;

    return ptr.element;
  }

  /**
   * Find an item in the tree.
   * 
   * @param x the item to search for.
   * @return the matching item of null if not found.
   */

  public Comparable find(Comparable x) {
    AANode current = root;

    while (current != nullNode) {
      int res = x.compareTo(current.element);
      if (res < 0) current = current.left;
      else if (res > 0) current = current.right;
      else return current.element;
    }
    return null;
  }

  /**
   * Make the tree logically empty.
   */
  public void clear() {
    root = nullNode;
  }

  /**
   * Test if the tree is logically empty.
   * 
   * @return true if empty, false otherwise.
   */
  public boolean isEmpty() {
    return root == nullNode;
  }

  public Iterator iterator() {
    return new AATreeIterator();
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
      inserted = false;
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
   * @throws ItemNotFoundException if x is not found.
   */
  private AANode remove(Comparable x, AANode t) {
    if (t != nullNode) {
      // Step 1: Search down the tree and set lastNode and deletedNode
      lastNode = t;
      if (x.compareTo(t.element) < 0) {
        t.left = remove(x, t.left);
      } else {
        deletedNode = t;
        t.right = remove(x, t.right);
      }

      // Step 2: If at the bottom of the tree and
      // x is present, we remove it
      if (t == lastNode) {
        if (deletedNode == nullNode || x.compareTo(deletedNode.element) != 0) {
          // XXX:: Modified to no throw ItemNotFoundException as we want to be able to remove elements without doing a
          // lookup.
          // throw new RuntimeException("ItemNotFoundException : " + x.toString());
        } else {
          deletedElement = deletedNode.element;
          deletedNode.element = t.element;
          t = t.right;
        }
      }

      // Step 3: Otherwise, we are not at the bottom; rebalance
      else if (t.left.level < t.level - 1 || t.right.level < t.level - 1) {
        if (t.right.level > --t.level) t.right.level = t.level;
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
    if (t.left.level == t.level) t = rotateWithLeftChild(t);
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
    return "AATree = { " + root.dump() + " }";
  }

  private static class AANode {
    // Constructors
    AANode(Comparable theElement) {
      element = theElement;
      left = right = nullNode;
      level = 1;
    }

    // XXX:: for debugging - costly operation
    public String dump() {
      String ds = String.valueOf(element);
      if (left != nullNode) {
        ds = ds + "," + left.dump();
      }
      if (right != nullNode) {
        ds = ds + "," + right.dump();
      }
      return ds;
    }

    Comparable element; // The data in the node
    AANode     left;   // Left child
    AANode     right;  // Right child
    int        level;  // Level

    public String toString() {
      return "AANode@" + System.identityHashCode(this) + "{" + element + "}";
    }
  }

  /*
   * This class is slightly inefficient in that it uses a stack internally to store state. But it is needed so that we
   * dont have to store parent references. Also it does not give the objects in sorted order (as it is just
   */
  private class AATreeIterator implements Iterator {

    // contains elements that needs to be travelled.
    List   s    = new ArrayList();
    AANode next = root;

    public boolean hasNext() {
      return (next != nullNode);
    }

    public Object next() {
      if (next == nullNode) { throw new NoSuchElementException(); }
      Object element = next.element;
      boolean leftNotNull = next.left != nullNode;
      boolean rightNotNull = next.right != nullNode;
      if (leftNotNull && rightNotNull) {
        s.add(next.right);
        next = next.left;
      } else if (leftNotNull) {
        next = next.left;
      } else if (rightNotNull) {
        next = next.right;
      } else if (s.size() > 0) {
        next = ((AANode) s.remove(s.size() - 1));
      } else {
        next = nullNode;
      }
      return element;
    }

    // This is a little tricky, the tree might rebalance itself.
    public void remove() {
      throw new ImplementMe();
    }

  }

  // Test program; should print min and max and nothing else
  // public static void main(String[] args) {
  // AATree t = new AATree();
  // final int NUMS = 400000;
  // final int GAP = 307;
  //
  // System.out.println("Checking... (no bad output means success)");
  //
  // t.insert(new Integer(NUMS * 2));
  // t.insert(new Integer(NUMS * 3));
  // for (int i = GAP; i != 0; i = (i + GAP) % NUMS)
  // t.insert(new Integer(i));
  // System.out.println("Inserts complete");
  //
  // t.remove(t.findMax());
  // for (int i = 1; i < NUMS; i += 2)
  // t.remove(new Integer(i));
  // t.remove(t.findMax());
  // System.out.println("Removes complete");
  //
  // if (((Integer) (t.findMin())).intValue() != 2 || ((Integer) (t.findMax())).intValue() != NUMS - 2) System.out
  // .println("FindMin or FindMax error!");
  //
  // for (int i = 2; i < NUMS; i += 2)
  // if (((Integer) t.find(new Integer(i))).intValue() != i) System.out.println("Error: find fails for " + i);
  //
  // for (int i = 1; i < NUMS; i += 2)
  // if (t.find(new Integer(i)) != null) System.out.println("Error: Found deleted item " + i);
  // }

  public static void main(String[] args) {
    AATree t = new AATree();
    System.out.println("Inserted = " + t.insert(new Integer(8)));
    System.out.println("Tree is       : " + t.dump());
    System.out.println("From Iterator : " + dumpUsingIterator(t));
    System.out.println("Inserted = " + t.insert(new Integer(4)));
    System.out.println("Tree is       : " + t.dump());
    System.out.println("From Iterator : " + dumpUsingIterator(t));
    System.out.println("Inserted = " + t.insert(new Integer(10)));
    System.out.println("Tree is       : " + t.dump());
    System.out.println("From Iterator : " + dumpUsingIterator(t));
    System.out.println("Inserted = " + t.insert(new Integer(2)));
    System.out.println("Tree is       : " + t.dump());
    System.out.println("From Iterator : " + dumpUsingIterator(t));
    System.out.println("Inserted = " + t.insert(new Integer(6)));
    System.out.println("Tree is       : " + t.dump());
    System.out.println("From Iterator : " + dumpUsingIterator(t));
    System.out.println("Inserted = " + t.insert(new Integer(9)));
    System.out.println("Tree is       : " + t.dump());
    System.out.println("From Iterator : " + dumpUsingIterator(t));
    System.out.println("Inserted = " + t.insert(new Integer(11)));
    System.out.println("Tree is       : " + t.dump());
    System.out.println("From Iterator : " + dumpUsingIterator(t));
    System.out.println("Inserted = " + t.insert(new Integer(1)));
    System.out.println("Tree is       : " + t.dump());
    System.out.println("From Iterator : " + dumpUsingIterator(t));
    System.out.println("Inserted = " + t.insert(new Integer(3)));
    System.out.println("Tree is       : " + t.dump());
    System.out.println("From Iterator : " + dumpUsingIterator(t));
    System.out.println("Inserted = " + t.insert(new Integer(5)));
    System.out.println("Tree is       : " + t.dump());
    System.out.println("From Iterator : " + dumpUsingIterator(t));
    System.out.println("Inserted = " + t.insert(new Integer(7)));
    System.out.println("Tree is       : " + t.dump());
    System.out.println("From Iterator : " + dumpUsingIterator(t));
    System.out.println("Inserted = " + t.insert(new Integer(12)));
    System.out.println("Tree is       : " + t.dump());
    System.out.println("From Iterator : " + dumpUsingIterator(t));
    System.out.println("Inserted = " + t.insert(new Integer(1)));
    System.out.println("Tree is       : " + t.dump());
    System.out.println("From Iterator : " + dumpUsingIterator(t));
    System.out.println("Inserted = " + t.insert(new Integer(3)));
    System.out.println("Tree is       : " + t.dump());
    System.out.println("From Iterator : " + dumpUsingIterator(t));

    System.out.println("Deleted = " + t.remove(new Integer(6)));
    System.out.println("Tree is       : " + t.dump());
    System.out.println("From Iterator : " + dumpUsingIterator(t));
    System.out.println("Deleted = " + t.remove(new Integer(8)));
    System.out.println("Tree is       : " + t.dump());
    System.out.println("From Iterator : " + dumpUsingIterator(t));
    System.out.println("Deleted = " + t.remove(new Integer(10)));
    System.out.println("Tree is       : " + t.dump());
    System.out.println("From Iterator : " + dumpUsingIterator(t));
    System.out.println("Deleted = " + t.remove(new Integer(12)));
    System.out.println("Tree is       : " + t.dump());
    System.out.println("From Iterator : " + dumpUsingIterator(t));
    System.out.println("Deleted = " + t.remove(new Integer(6)));
    System.out.println("Tree is       : " + t.dump());
    System.out.println("From Iterator : " + dumpUsingIterator(t));
    System.out.println("Deleted = " + t.remove(new Integer(8)));
    System.out.println("Tree is       : " + t.dump());
    System.out.println("From Iterator : " + dumpUsingIterator(t));
    System.out.println("Deleted = " + t.remove(new Integer(1)));
    System.out.println("Tree is       : " + t.dump());
    System.out.println("From Iterator : " + dumpUsingIterator(t));

  }

  private static String dumpUsingIterator(AATree t) {
    StringBuffer sb = new StringBuffer();
    for (Iterator i = t.iterator(); i.hasNext();) {
      sb.append(i.next());
      if (i.hasNext()) {
        sb.append(',');
      }
    }
    return sb.toString();
  }
}