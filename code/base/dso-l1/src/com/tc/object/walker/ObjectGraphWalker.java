/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.walker;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.Map;

public class ObjectGraphWalker {

  private final VisitedSet  visited;
  private final LinkedList  backtrack    = new LinkedList();
  private final Visitor     visitor;
  private final MemberValue root;
  private int               currentDepth = 0;
  private int               maxDepth     = -1;
  private final WalkTest    walkTest;

  public ObjectGraphWalker(Object root, WalkTest walkTest, Visitor visitor) {
    if (root == null) { throw new IllegalArgumentException("refusing to traverse null"); }
    if (walkTest == null) { throw new NullPointerException("null walk test"); }
    if (visitor == null) { throw new NullPointerException("null visitor"); }
    this.root = MemberValue.rootValue(root);
    this.visitor = visitor;
    this.visited = new VisitedSet(this.root);
    this.walkTest = walkTest;
  }

  public void setMaxDepth(int maxDepth) {
    this.maxDepth = maxDepth;
  }

  public void walk() {
    visitor.visitRootObject(root);
    currentDepth++;

    if (!walkTest.shouldTraverse(root)) { return; }

    backtrack.addFirst(makeNode(root.getValueObject()));

    while (backtrack.size() > 0) {
      Node current = (Node) backtrack.getFirst();
      visit(current);
    }
  }

  private Node makeNode(Object o) {
    if (o == null) { throw new NullPointerException(); }

    if (o.getClass().isArray()) {
      return new ArrayNode(o);
    } else if (o instanceof Collection) {
      return new CollectionNode((Collection) o, walkTest);
    } else if (o instanceof Map) {
      return new MapNode((Map) o, walkTest);
    } else if (o instanceof MapEntry) {
      return new MapEntryNode((MapEntry) o);
    } else {
      return new PlainNode(o, walkTest);
    }
  }

  private void visit(Node node) {
    while (!node.done()) {
      MemberValue value = node.next();

      if (value instanceof MapEntry) {
        MapEntry entry = (MapEntry) value;
        visitor.visitMapEntry(entry.getIndex(), currentDepth);
        currentDepth++;
        backtrack.addFirst(makeNode(entry));
        return;
      }

      Object o = value.getValueObject();

      boolean shouldTraverse = (maxDepth <= 0 || currentDepth < maxDepth) && walkTest.shouldTraverse(value);

      if (o != null && shouldTraverse) {
        visited.visit(value);
      }

      visitor.visitValue(value, currentDepth);

      if (o != null && shouldTraverse && !value.isRepeated()) {
        Node next = makeNode(o);
        currentDepth++;
        backtrack.addFirst(next);
        return;
      }
    }

    currentDepth--;
    backtrack.removeFirst();
  }

  private static class VisitedSet {
    private final IdentityHashMap visited = new IdentityHashMap();

    VisitedSet(MemberValue root) {
      visit(root);
    }

    void visit(MemberValue value) {
      Object o = value.getValueObject();
      if (o == null) { throw new AssertionError("null value not expected"); }

      Integer id = (Integer) visited.get(o);
      if (id == null) {
        id = new Integer(visited.size());
        visited.put(o, id);
      } else {
        value.setRepeated(true);
      }

      value.setId(id.intValue());
    }
  }
}
