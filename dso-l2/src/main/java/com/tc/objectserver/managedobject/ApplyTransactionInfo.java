/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.object.ObjectID;
import com.tc.util.ObjectIDSet;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class ApplyTransactionInfo {

  private final Map     nodes;
  private final Set     parents;
  private final boolean activeTxn;
  private Set<ObjectID> ignoreBroadcasts = Collections.EMPTY_SET;
  private Set<ObjectID> initiateEviction = Collections.EMPTY_SET;
  private Set<ObjectID> invalidate       = Collections.EMPTY_SET;

  // For tests
  public ApplyTransactionInfo() {
    this(true);
  }

  public ApplyTransactionInfo(final boolean activeTxn) {
    this.activeTxn = activeTxn;
    this.parents = new HashSet();
    this.nodes = new HashMap();
  }

  public void addBackReference(final ObjectID child, final ObjectID parent) {
    if (child.isNull()) { return; }
    final Node c = getOrCreateNode(child);
    final Node p = getOrCreateNode(parent);
    p.addChild(c);
    this.parents.add(parent);
  }

  private Node getOrCreateNode(final ObjectID id) {
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

  public Set addReferencedChildrenTo(final Set objectIDs, final Set interestedParents) {
    for (final Iterator i = interestedParents.iterator(); i.hasNext();) {
      final ObjectID pid = (ObjectID) i.next();
      final Node p = getOrCreateNode(pid);
      p.addAllReferencedChildrenTo(objectIDs);
    }
    return objectIDs;
  }

  private static class Node {

    private final ObjectID id;
    private final Set      children;

    public Node(final ObjectID id) {
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
    public boolean equals(final Object o) {
      if (o instanceof Node) {
        final Node other = (Node) o;
        return this.id.equals(other.id);
      }
      return false;
    }

    public void addChild(final Node c) {
      this.children.add(c);
    }

    public Set addAllReferencedChildrenTo(final Set objectIDs) {
      for (final Iterator i = this.children.iterator(); i.hasNext();) {
        final Node child = (Node) i.next();
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

  public boolean isActiveTxn() {
    return this.activeTxn;
  }

  public void ignoreBroadcastFor(final ObjectID objectID) {
    if (this.ignoreBroadcasts == Collections.EMPTY_SET) {
      this.ignoreBroadcasts = new ObjectIDSet();
    }
    this.ignoreBroadcasts.add(objectID);
  }

  public boolean isBroadcastIgnoredFor(final ObjectID oid) {
    return this.ignoreBroadcasts.contains(oid);
  }

  public void initiateEvictionFor(final ObjectID objectID) {
    if (this.initiateEviction == Collections.EMPTY_SET) {
      this.initiateEviction = new ObjectIDSet();
    }
    this.initiateEviction.add(objectID);
  }

  public Set getObjectIDsToInitateEviction() {
    return this.initiateEviction;
  }

  public void invalidate(ObjectID old) {
    if (this.invalidate == Collections.EMPTY_SET) {
      this.invalidate = new ObjectIDSet();
    }
    this.invalidate.add(old);
  }

  public Set<ObjectID> getObjectIDsToInvalidate() {
    return invalidate;
  }
}
