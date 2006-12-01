/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.perf.collections;

import java.util.Arrays;
import java.util.Vector;

public interface ElementType extends Comparable {
  public void traverse();

  public interface Factory {
    ElementType create();

    String describeType();
  }

  public static class LongFactory implements Factory {
    long next = 0;

    public ElementType create() {
      Long l = new Long(next++);
      return new WrappedComparable(l);
    }

    public String describeType() {
      return "Long";
    }

  }

  public static class StringFactory implements Factory {
    long   next   = 0;
    String prefix = "standard_";

    public void setPrefix(String newPrefix) {
      prefix = newPrefix;
    }

    public String describeType() {
      return "String";
    }

    public ElementType create() {
      return new WrappedComparable(prefix + next++);
    }
  }

  public static class GraphFactory implements Factory {
    ElementType.Factory contentFactory = null;
    int                 graphSize      = 10;

    public GraphFactory(int size, ElementType.Factory valueFactory) {
      graphSize = size;
      contentFactory = valueFactory;
    }

    public ElementType create() {
      return new GraphType(graphSize, contentFactory);
    }

    public String describeType() {
      StringBuffer buf = new StringBuffer("BinaryTree(");
      buf.append(contentFactory.describeType());
      buf.append("[");
      buf.append(graphSize);
      buf.append("])");
      return buf.toString();
    }

  }

  public class GraphType implements ElementType { // simple lazy balanced binary tree minus insert, remove, etc.
    class Node {
      Node        right = null, left = null;
      ElementType value;

      Node(ElementType e) {
        value = e;
      }

      // in-order contents
      void addContents(Vector result) {
        if (left != null) left.addContents(result);
        result.add(value);
        if (right != null) right.addContents(result);
      }
    }

    // in order contents
    Vector getContents() {
      Vector ret = new Vector();
      if (top != null) top.addContents(ret);
      return ret;
    }

    static int atMostCompare = 100;

    public int compareTo(Object other) {
      if (!(other instanceof GraphType)) return 0;

      Vector contents1 = getContents();
      Vector contents2 = ((GraphType) other).getContents();
      int cnt1 = contents1.size(), cnt2 = contents2.size();
      int cnt = Math.min(cnt1, cnt2);
      if (cnt == 0) return (cnt1 > 0) ? 1 : ((cnt2 > 0) ? -1 : 0);
      else {
        int cmp = 0;
        for (int i = 0; (cmp == 0) && (i < cnt); i++) {

          cmp = ((Comparable) contents1.get(i)).compareTo(contents2.get(i));

        }
        return cmp;
      }
    }

    // utility method
    ElementType[] subArray(int start, int end, ElementType[] original) {
      int size = end - start + 1;
      ElementType[] ret = new ElementType[size];
      for (int i = 0; i < size; i++)
        ret[i] = original[i + start];
      return ret;
    }

    // simple construction from array of values
    Node nodeFromCollection(ElementType[] collection) {
      int size = collection.length;
      if (size == 0) return null;
      int split = size / 2;
      Node ret = new Node(collection[split]);
      ret.left = nodeFromCollection(subArray(0, split - 1, collection));
      ret.right = nodeFromCollection(subArray(split + 1, size - 1, collection));
      return ret;
    }

    Node                top = null;
    ElementType.Factory elementFactory;

    public void addNode(ElementType value) {
      // TBD: not used yet
      // Node newNode = new Node(value);
    }

    public GraphType(int size, ElementType.Factory factory) {
      elementFactory = factory;
      ElementType[] contents = new ElementType[size];
      for (int i = 0; i < size; i++)
        contents[i] = elementFactory.create();
      Arrays.sort(contents);
      top = nodeFromCollection(contents);
    }

    void traverse(Node node) {
      if (node != null) {
        traverse(node.left);
        node.value.traverse();
        traverse(node.right);
      }
    }

    public void traverse() {
      traverse(top);
    }
  }

  // useful for Numeric, String, Date, etc. types
  public class WrappedComparable implements ElementType {
    Comparable wrapped;

    public Comparable getWrapped() {
      return wrapped;
    }

    public WrappedComparable(Comparable c) {
      wrapped = c;
    }

    public void traverse() {
      // nothing yet
    }

    public int compareTo(Object other) {
      return (other instanceof WrappedComparable) ? ((WrappedComparable) other).getWrapped().compareTo(wrapped) : 0;
    }
  }

}
