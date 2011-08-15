/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */

package com.tc.util;

import java.util.AbstractSet;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.SortedSet;

public class AATreeSet<T extends Comparable> extends AbstractSet<T> implements SortedSet<T> {

  private Node<T> root = terminal();

  private int     size = 0;
  private boolean mutated;

  private Node<T> item = terminal(), heir = terminal();
  private T       removed;

  @Override
  public boolean add(T o) {
    try {
      root = insert(root, o);
      if (mutated) {
        size++;
      }
      return mutated;
    } finally {
      mutated = false;
    }
  }

  @Override
  public boolean remove(Object o) {
    try {
      root = remove(root, (T) o);
      if (mutated) {
        size--;
      }
      return mutated;
    } finally {
      heir = terminal();
      item = terminal();
      mutated = false;
      removed = null;
    }
  }

  public T removeAndReturn(Object o) {
    try {
      root = remove(root, (T) o);
      if (mutated) {
        size--;
      }
      return removed;
    } finally {
      heir = terminal();
      item = terminal();
      mutated = false;
      removed = null;
    }
  }

  @Override
  public void clear() {
    root = terminal();
    size = 0;
  }

  @Override
  public Iterator<T> iterator() {
    return new TreeIterator();
  }

  @Override
  public int size() {
    return size;
  }

  @Override
  public boolean isEmpty() {
    return root == terminal();
  }

  public Comparator<? super T> comparator() {
    return null;
  }

  public SortedSet<T> subSet(T fromElement, T toElement) {
    return new SubSet(fromElement, toElement);
  }

  public SortedSet<T> headSet(T toElement) {
    return new SubSet(null, toElement);
  }

  public SortedSet<T> tailSet(T fromElement) {
    return new SubSet(fromElement, null);
  }

  public T first() {
    Node<T> leftMost = root;
    while (leftMost.getLeft() != terminal()) {
      leftMost = leftMost.getLeft();
    }
    return leftMost.getPayload();
  }

  public T last() {
    Node<T> rightMost = root;
    while (rightMost.getRight() != terminal()) {
      rightMost = rightMost.getRight();
    }
    return rightMost.getPayload();
  }

  public T find(Object probe) {
    return find(root, (T) probe).getPayload();
  }

  private static final Node<?> TERMINAL = new TerminalNode();

  private Node<T> terminal() {
    return (Node<T>) TERMINAL;
  }

  protected final Node<T> getRoot() {
    return root;
  }

  private Node<T> find(Node<T> top, T probe) {
    if (top == terminal()) {
      return top;
    } else {
      int direction = top.compareTo(probe);
      if (direction > 0) {
        return find(top.getLeft(), probe);
      } else if (direction < 0) {
        return find(top.getRight(), probe);
      } else {
        return top;
      }
    }
  }

  private Node<T> insert(Node<T> top, T data) {
    if (top == terminal()) {
      mutated = true;
      return createNode(data);
    } else {
      int direction = top.compareTo(data);
      if (direction > 0) {
        top.setLeft(insert(top.getLeft(), data));
      } else if (direction < 0) {
        top.setRight(insert(top.getRight(), data));
      } else {
        return top;
      }
      top = skew(top);
      top = split(top);
      return top;
    }
  }

  private Node<T> createNode(T data) {
    if (data instanceof Node<?>) {
      return (Node<T>) data;
    } else {
      return new TreeNode<T>(data);
    }
  }

  private Node<T> remove(Node<T> top, T data) {
    if (top != terminal()) {
      int direction = top.compareTo(data);

      heir = top;
      if (direction > 0) {
        top.setLeft(remove(top.getLeft(), data));
      } else {
        item = top;
        top.setRight(remove(top.getRight(), data));
      }

      if (top == heir) {
        if (item != terminal() && item.compareTo(data) == 0) {
          mutated = true;
          item.swapPayload(top);
          removed = top.getPayload();
          top = top.getRight();
        }
      } else {
        if (top.getLeft().getLevel() < top.getLevel() - 1 || top.getRight().getLevel() < top.getLevel() - 1) {
          if (top.getRight().getLevel() > top.decrementLevel()) {
            top.getRight().setLevel(top.getLevel());
          }

          top = skew(top);
          top.setRight(skew(top.getRight()));
          top.getRight().setRight(skew(top.getRight().getRight()));
          top = split(top);
          top.setRight(split(top.getRight()));
        }
      }
    }
    return top;
  }

  private static <T> Node<T> skew(Node<T> top) {
    if (top.getLeft().getLevel() == top.getLevel() && top.getLevel() != 0) {
      Node<T> save = top.getLeft();
      top.setLeft(save.getRight());
      save.setRight(top);
      top = save;
    }

    return top;
  }

  private static <T> Node<T> split(Node<T> top) {
    if (top.getRight().getRight().getLevel() == top.getLevel() && top.getLevel() != 0) {
      Node<T> save = top.getRight();
      top.setRight(save.getLeft());
      save.setLeft(top);
      top = save;
      top.incrementLevel();
    }

    return top;
  }

  public static interface Node<E> {

    public int compareTo(E data);

    public void setLeft(Node<E> node);

    public void setRight(Node<E> node);

    public Node<E> getLeft();

    public Node<E> getRight();

    public int getLevel();

    public void setLevel(int value);

    public int decrementLevel();

    public int incrementLevel();

    public void swapPayload(Node<E> with);

    public E getPayload();
  }

  public static abstract class AbstractTreeNode<E> implements Node<E> {

    private Node<E> left;
    private Node<E> right;
    private int     level;

    public AbstractTreeNode() {
      this(1);
    }

    private AbstractTreeNode(int level) {
      this.left = (Node<E>) TERMINAL;
      this.right = (Node<E>) TERMINAL;
      this.level = level;
    }

    public void setLeft(Node<E> node) {
      left = node;
    }

    public void setRight(Node<E> node) {
      right = node;
    }

    public Node<E> getLeft() {
      return left;
    }

    public Node<E> getRight() {
      return right;
    }

    public int getLevel() {
      return level;
    }

    public void setLevel(int value) {
      level = value;
    }

    public int decrementLevel() {
      return --level;
    }

    public int incrementLevel() {
      return ++level;
    }
  }

  private static final class TreeNode<E extends Comparable> extends AbstractTreeNode<E> {

    private E payload;

    public TreeNode(E payload) {
      super();
      this.payload = payload;
    }

    public int compareTo(E data) {
      return payload.compareTo(data);
    }

    public void swapPayload(Node<E> node) {
      if (node instanceof TreeNode<?>) {
        TreeNode<E> treeNode = (TreeNode<E>) node;
        E temp = treeNode.payload;
        treeNode.payload = this.payload;
        this.payload = temp;
      } else {
        throw new IllegalArgumentException();
      }
    }

    public E getPayload() {
      return payload;
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder("TreeNode ");
      sb.append("level:").append(getLevel());
      sb.append(" payload:" + getPayload());
      return sb.toString();
    }
  }

  private static final class TerminalNode<E> extends AbstractTreeNode<E> {

    TerminalNode() {
      super(0);
      super.setLeft(this);
      super.setRight(this);
    }

    public int compareTo(E data) {
      return 0;
    }

    @Override
    public void setLeft(Node<E> right) {
      if (right != this) { throw new AssertionError(); }
    }

    @Override
    public void setRight(Node<E> left) {
      if (left != this) { throw new AssertionError(); }
    }

    @Override
    public void setLevel(int value) {
      throw new AssertionError();
    }

    @Override
    public int decrementLevel() {
      throw new AssertionError();
    }

    @Override
    public int incrementLevel() {
      throw new AssertionError();
    }

    public void swapPayload(Node<E> payload) {
      throw new AssertionError();
    }

    public E getPayload() {
      return null;
    }
  }

  class SubSet extends AbstractSet<T> implements SortedSet<T> {

    private final T start;
    private final T end;

    SubSet(T start, T end) {
      this.start = start;
      this.end = end;
    }

    @Override
    public boolean add(T o) {
      if (inRange(o)) {
        return AATreeSet.this.add(o);
      } else {
        throw new IllegalArgumentException();
      }
    }

    @Override
    public boolean remove(Object o) {
      if (inRange((T) o)) {
        return remove(o);
      } else {
        return false;
      }
    }

    @Override
    public void clear() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<T> iterator() {
      return new SubTreeIterator(start, end);
    }

    @Override
    public int size() {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEmpty() {
      return !iterator().hasNext();
    }

    public Comparator<? super T> comparator() {
      return null;
    }

    public SortedSet<T> subSet(T fromElement, T toElement) {
      if (inRangeInclusive(fromElement) && inRangeInclusive(toElement)) {
        return new SubSet(fromElement, toElement);
      } else {
        throw new IllegalArgumentException();
      }
    }

    public SortedSet<T> headSet(T toElement) {
      if (inRangeInclusive(toElement)) {
        return new SubSet(start, toElement);
      } else {
        throw new IllegalArgumentException();
      }
    }

    public SortedSet<T> tailSet(T fromElement) {
      if (inRangeInclusive(fromElement)) {
        return new SubSet(fromElement, end);
      } else {
        throw new IllegalArgumentException();
      }
    }

    public T first() {
      if (start == null) {
        return AATreeSet.this.first();
      } else {
        throw new UnsupportedOperationException();
      }
    }

    public T last() {
      if (end == null) {
        return AATreeSet.this.last();
      } else {
        throw new UnsupportedOperationException();
      }
    }

    private boolean inRange(T value) {
      return (start == null || start.compareTo(value) <= 0) && (end == null || end.compareTo(value) > 0);
    }

    private boolean inRangeInclusive(T value) {
      return (start == null || start.compareTo(value) <= 0) && (end == null || end.compareTo(value) >= 0);
    }
  }

  class TreeIterator implements Iterator<T> {

    private final Stack<Node<T>> path = new Stack<Node<T>>();
    protected Node<T>            next;

    TreeIterator() {
      path.push(terminal());
      Node<T> leftMost = root;
      while (leftMost.getLeft() != terminal()) {
        path.push(leftMost);
        leftMost = leftMost.getLeft();
      }
      next = leftMost;
    }

    TreeIterator(T start) {
      path.push(terminal());
      Node<T> current = root;
      while (true) {
        int direction = current.compareTo(start);
        if (direction > 0) {
          if (current.getLeft() == terminal()) {
            next = current;
            break;
          } else {
            path.push(current);
            current = current.getLeft();
          }
        } else if (direction < 0) {
          if (current.getRight() == terminal()) {
            next = path.pop();
            break;
          } else {
            current = current.getRight();
          }
        } else {
          next = current;
          break;
        }
      }
    }

    public boolean hasNext() {
      return next != terminal();
    }

    public T next() {
      Node<T> current = next;
      advance();
      return current.getPayload();
    }

    private void advance() {
      Node<T> successor = next.getRight();
      if (successor != terminal()) {
        while (successor.getLeft() != terminal()) {
          path.push(successor);
          successor = successor.getLeft();
        }
        next = successor;
      } else {
        next = path.pop();
      }
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }

  }

  class SubTreeIterator extends TreeIterator {
    private final Node<T> terminalNode;

    public SubTreeIterator(T start, T end) {
      super(start);
      if (end != null) {
        Stack<Node<T>> path = new Stack<Node<T>>();
        path.push(terminal());
        Node<T> current = root;
        while (true) {
          int direction = current.compareTo(start);
          if (direction > 0) {
            if (current.getLeft() == terminal()) {
              terminalNode = current;
              break;
            } else {
              path.push(current);
              current = current.getLeft();
            }
          } else if (direction < 0) {
            if (current.getRight() == terminal()) {
              terminalNode = path.pop();
              break;
            } else {
              current = current.getRight();
            }
          } else {
            terminalNode = current;
            break;
          }
        }
      } else {
        terminalNode = terminal();
      }
    }

    @Override
    public boolean hasNext() {
      return super.hasNext() && next != terminalNode;
    }

    @Override
    public T next() {
      if (next == terminalNode) {
        throw new NoSuchElementException();
      } else {
        return super.next();
      }
    }

  }

  /**
   * Validates that this is a correctly balanced AA Tree.
   * <p>
   * Rules for an AA Tree:
   * <ol>
   * <li>Every path contains the same number of pseudo-nodes.</li>
   * <li>A left child may not have the same level as its parent.</li>
   * <li>Two right children with the same level as the parent are not allowed.</li>
   * </ol>
   */
  public void validateTreeStructure() {
    validateNode(root);
  }

  private static void validateNode(Node<?> node) {
    if (node == TERMINAL) { return; }

    Node<?> left = node.getLeft();
    Node<?> right = node.getRight();
    Node<?> rightRight = right.getRight();

    if (left.getLevel() == node.getLevel()) {
      throw new AssertionError(node + " has the same level as it's left child: " + left);
    } else if (node.getLevel() == right.getLevel() && right.getLevel() == rightRight.getLevel()) {
      throw new AssertionError(node + " has the two successive right children with the same level: " + right + ", "
                               + rightRight);
    } else if (left == TERMINAL && right == TERMINAL && node.getLevel() != 1) {
      throw new AssertionError(node + " is a leaf node but has an invalid level");
    } else if (left != TERMINAL && left.getLevel() != node.getLevel() - 1) {
      throw new AssertionError(node + " has a left child with an invalid level: " + left);
    } else if (right != TERMINAL && right.getLevel() != node.getLevel() && right.getLevel() != node.getLevel() - 1) {
      throw new AssertionError(node + " has a right child with an invalid level: " + right);
    } else {
      validateNode(left);
      validateNode(right);
    }
  }
}
