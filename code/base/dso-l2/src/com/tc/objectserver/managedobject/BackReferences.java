/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.object.ObjectID;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

//TODO::This class doesnt maintain backreferences anymore. Should be renamed.
public class BackReferences {

  private final Map nodes;

  private final Set parents;

  public BackReferences() {
    parents = new HashSet();
    nodes = new HashMap();
  }

  public void addBackReference(ObjectID child, ObjectID parent) {
    if (child.isNull()) return;
    Node c = getOrCreateNode(child);
    Node p = getOrCreateNode(parent);
    p.addChild(c);
    parents.add(parent);
  }

  private Node getOrCreateNode(ObjectID id) {
    Node n = (Node) nodes.get(id);
    if (n == null) {
      n = new Node(id);
      nodes.put(id, n);
    }
    return n;
  }

  public Set getAllParents() {
    return new HashSet(parents);
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

    public int hashCode() {
      return id.hashCode();
    }
    
    public ObjectID getID() {
      return id;
    }

    public boolean equals(Object o) {
      if (o instanceof Node) {
        Node other = (Node) o;
        return this.id.equals(other.id);
      }
      return false;
    }

    public void addChild(Node c) {
      children.add(c);
    }
    
    public Set addAllReferencedChildrenTo(Set objectIDs) {
      for (Iterator i = children.iterator(); i.hasNext();) {
        Node child = (Node) i.next();
        if(!objectIDs.contains(child.getID())) {
          objectIDs.add(child.getID());
          child.addAllReferencedChildrenTo(objectIDs);
        }
      }
      return objectIDs;
    }

    public String toString() {
      // XXX:: dont just print the contents of children. That might cause a recursive loop
      return "Node(" + id + ") : children = " + children.size();
    }
  }

}
