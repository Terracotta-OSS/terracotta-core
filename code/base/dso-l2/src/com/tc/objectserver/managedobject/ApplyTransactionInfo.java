/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.object.ObjectID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.util.ObjectIDSet;
import com.tc.util.TCCollections;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

public class ApplyTransactionInfo {

  private final Map<ObjectID, Node> nodes;
  private final Set<ObjectID>       parents;
  private final ServerTransactionID stxnID;
  private final boolean             isActiveTxn;
  private Set<ObjectID>             ignoreBroadcasts = Collections.EMPTY_SET;
  private Set<ObjectID>             initiateEviction = Collections.EMPTY_SET;
  private Set<ObjectID>             invalidate       = Collections.EMPTY_SET;
  private SortedSet<ObjectID>       deleteObjects    = TCCollections.EMPTY_SORTED_SET;

  // For tests
  public ApplyTransactionInfo() {
    this(true, ServerTransactionID.NULL_ID);
  }

  public ApplyTransactionInfo(final boolean isActiveTxn, final ServerTransactionID stxnID) {
    this.isActiveTxn = isActiveTxn;
    this.stxnID = stxnID;
    this.parents = new ObjectIDSet();
    this.nodes = new HashMap<ObjectID, Node>();
  }

  public void addBackReference(final ObjectID child, final ObjectID parent) {
    if (child.isNull()) { return; }
    final Node c = getOrCreateNode(child);
    final Node p = getOrCreateNode(parent);
    p.addChild(c);
    this.parents.add(parent);
  }

  private Node getOrCreateNode(final ObjectID id) {
    Node n = this.nodes.get(id);
    if (n == null) {
      n = new Node(id);
      this.nodes.put(id, n);
    }
    return n;
  }

  public Set<ObjectID> getAllParents() {
    return new ObjectIDSet(this.parents);
  }

  public Set<ObjectID> addReferencedChildrenTo(final Set<ObjectID> objectIDs, final Set<ObjectID> interestedParents) {
    for (ObjectID pid : interestedParents) {
      final Node p = getOrCreateNode(pid);
      p.addAllReferencedChildrenTo(objectIDs);
    }
    return objectIDs;
  }

  private static class Node {

    private final ObjectID  id;
    private final Set<Node> children;

    public Node(final ObjectID id) {
      this.id = id;
      this.children = new HashSet<Node>();
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

    public Set<ObjectID> addAllReferencedChildrenTo(final Set<ObjectID> objectIDs) {
      for (final Node child : this.children) {
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
    return isActiveTxn;
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

  public void deleteObject(ObjectID old) {
    if (this.deleteObjects == TCCollections.EMPTY_SORTED_SET) {
      this.deleteObjects = new ObjectIDSet();
    }
    this.deleteObjects.add(old);
  }

  public SortedSet<ObjectID> getObjectIDsToDelete() {
    return deleteObjects;
  }

  public ServerTransactionID getServerTransactionID() {
    return stxnID;
  }
}
