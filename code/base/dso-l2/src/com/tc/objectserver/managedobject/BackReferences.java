/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.object.ObjectID;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

// TODO::This class doesnt maintain backreferences anymore. Should be renamed.
public class BackReferences {

  private final Map nodes;

  private final Set parents;

  public BackReferences() {
    this.parents = new HashSet();
    this.nodes = new HashMap();
  }

  public void addBackReference(ObjectID child, ObjectID parent) {
    if (child.isNull()) { return; }
    Node c = getOrCreateNode(child);
    Node p = getOrCreateNode(parent);
    p.addChild(c);
    this.parents.add(parent);
  }

  private Node getOrCreateNode(ObjectID id) {
    Node n = (Node) this.nodes.get(id);
    if (n == null) {
      n = new Node(id);
      this.nodes.put(id, n);
    }
    return n;
  }

  public Set getAllParents() {
    return new HashSet(this.parents);
  }

  public Set addReferencedChildrenTo(Set objectIDs, Set interestedParents) {
    for (Iterator i = interestedParents.iterator(); i.hasNext();) {
      ObjectID pid = (ObjectID) i.next();
      Node p = getOrCreateNode(pid);
      p.addAllReferencedChildrenTo(objectIDs);
    }
    return objectIDs;
  }

  private static class Node {

    private final ObjectID id;
    private final Set      children;

    public Node(ObjectID id) {
      this.id = id;
      this.children = new HashSet();
    }

    @Override
    public int hashCode() {
      return this.id.hashCode();
    }

    public ObjectID getID() {
      return this.id;
    }

    @Override
    public boolean equals(Object o) {
      if (o instanceof Node) {
        Node other = (Node) o;
        return this.id.equals(other.id);
      }
      return false;
    }

    public void addChild(Node c) {
      this.children.add(c);
    }

    public Set addAllReferencedChildrenTo(Set objectIDs) {
      for (Iterator i = this.children.iterator(); i.hasNext();) {
        Node child = (Node) i.next();
        if (objectIDs.add(child.getID())) {
          child.addAllReferencedChildrenTo(objectIDs);
        }
      }
      return objectIDs;
    }

    @Override
    public String toString() {
      // Don't just print the contents of children. That might cause a recursive loop
      return "Node(" + this.id + ") : children = " + this.children.size();
    }
  }

}
