/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.google.common.collect.Sets;
import com.tc.invalidation.Invalidations;
import com.tc.object.ObjectID;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.event.ClientChannelMonitor;
import com.tc.objectserver.event.DefaultMutationEventPublisher;
import com.tc.objectserver.event.InClusterServerEventBuffer;
import com.tc.objectserver.event.MutationEventPublisher;
import com.tc.objectserver.event.ServerEventBuffer;
import com.tc.util.BitSetObjectIDSet;
import com.tc.util.TCCollections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

public class ApplyTransactionInfo {

  private final Map<ObjectID, Node>    nodes;
  private final Set<ObjectID>          parents;
  private final ServerTransactionID    stxnID;
  private final boolean isEviction;
  private final boolean                isActiveTxn;
  private Set<ObjectID>                ignoreBroadcasts   = Collections.emptySet();
  private Set<ObjectID>                initiateEviction   = Collections.emptySet();
  private SortedSet<ObjectID>          deleteObjects      = TCCollections.EMPTY_SORTED_SET;
  // TODO: This is probably not the place to pass releaseable objects...
  private Collection<ManagedObject>    objectsToRelease   = Collections.emptySet();
  private Invalidations                invalidate;
  private final boolean                isSearchEnabled;
  private final Map<ObjectID, Boolean> keyPresentForValue = new HashMap<ObjectID, Boolean>();
  private boolean                      commitNow;
  private final MutationEventPublisher mutationEventPublisher;
  private final ApplyResultRecorder    resultRecorder;
  private Set<ObjectID>                echoChangesFor = TCCollections.EMPTY_OBJECT_ID_SET;
  private final ServerEventBuffer      serverEventBuffer;
  private final ClientChannelMonitor   clientChannelMonitor;

  // For tests
  public ApplyTransactionInfo() {
    this(true, ServerTransactionID.NULL_ID, GlobalTransactionID.NULL_ID, false, false,
         new InClusterServerEventBuffer(), null);
  }

  public ApplyTransactionInfo(final boolean isActiveTxn, final ServerTransactionID stxnID,
                              final GlobalTransactionID gtxId, final boolean isSearchEnabled, final boolean isEviction,
                              final ServerEventBuffer serverEventBuffer, final ClientChannelMonitor clientChannelMonitor) {
    this.isActiveTxn = isActiveTxn;
    this.stxnID = stxnID;
    this.isEviction = isEviction;
    this.parents = new BitSetObjectIDSet();
    this.nodes = new HashMap<ObjectID, Node>();
    this.isSearchEnabled = isSearchEnabled;
    this.serverEventBuffer = serverEventBuffer;
    this.clientChannelMonitor = clientChannelMonitor;
    this.mutationEventPublisher = new DefaultMutationEventPublisher(gtxId, serverEventBuffer);
    this.resultRecorder = new DefaultResultRecorderImpl();
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
    return new BitSetObjectIDSet(this.parents);
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
      this.ignoreBroadcasts = new BitSetObjectIDSet();
    }
    this.ignoreBroadcasts.add(objectID);
  }

  public boolean isBroadcastIgnoredFor(final ObjectID oid) {
    return this.ignoreBroadcasts.contains(oid);
  }

  public void initiateEvictionFor(final ObjectID objectID) {
    if (this.initiateEviction == Collections.EMPTY_SET) {
      this.initiateEviction = new BitSetObjectIDSet();
    }
    this.initiateEviction.add(objectID);
  }

  public Set<ObjectID> getObjectIDsToInitateEviction() {
    return this.initiateEviction;
  }

  public void invalidate(ObjectID mapID, ObjectID old) {
    if (this.invalidate == null) {
      this.invalidate = new Invalidations();
    }
    this.invalidate.add(mapID, old);
  }

  public Invalidations getObjectIDsToInvalidate() {
    return invalidate;
  }

  public void deleteObject(ObjectID old) {
    if (this.deleteObjects == TCCollections.EMPTY_SORTED_SET) {
      this.deleteObjects = new BitSetObjectIDSet();
    }
    this.deleteObjects.add(old);
  }

  public void deleteObjects(Set<ObjectID> oids) {
    if (this.deleteObjects == TCCollections.EMPTY_SORTED_SET) {
      this.deleteObjects = new BitSetObjectIDSet();
    }
    this.deleteObjects.addAll(oids);
  }
  
  public boolean hasObjectsToDelete() {
    return !deleteObjects.isEmpty();
  }

  public SortedSet<ObjectID> getObjectIDsToDelete() {
    return deleteObjects;
  }

  public ServerTransactionID getServerTransactionID() {
    return stxnID;
  }

  public Boolean getKeyStatusForValue(ObjectID value) {
    return this.keyPresentForValue.get(value);
  }

  public void recordValue(ObjectID val, boolean keyExists) {
    this.keyPresentForValue.put(val, keyExists);
  }

  public boolean isSearchEnabled() {
    return isSearchEnabled;
  }

  public void addObjectsToBeReleased(Collection<ManagedObject> objects) {
    if (objectsToRelease == Collections.EMPTY_SET) {
      objectsToRelease = new ArrayList<ManagedObject>(objects);
    } else {
      objectsToRelease.addAll(objects);
    }
  }

  public Collection<ManagedObject> getObjectsToRelease() {
    return objectsToRelease;
  }

  public boolean isCommitNow() {
    return commitNow;
  }

  public void setCommitNow(final boolean commitNow) {
    this.commitNow = commitNow;
  }

  public void removeKeyPresentForValue(ObjectID value) {
    keyPresentForValue.remove(value);
  }

  public MutationEventPublisher getMutationEventPublisher() {
    return mutationEventPublisher;
  }

  public ApplyResultRecorder getApplyResultRecorder() {
    return resultRecorder;
  }

  public boolean isEviction() {
    return isEviction;
  }

  public void echoChangesFor(ObjectID objectID) {
    if (echoChangesFor == TCCollections.EMPTY_OBJECT_ID_SET) {
      echoChangesFor = Sets.newHashSet();
    }
    echoChangesFor.add(objectID);
  }

  public Set<ObjectID> getObjectsToEchoChangesFor() {
    return echoChangesFor;
  }
  
  public ServerEventBuffer getServerEventBuffer() {
    return serverEventBuffer;
  }

  public ClientChannelMonitor getClientChannelMonitor() {
    return clientChannelMonitor;
  }
}
