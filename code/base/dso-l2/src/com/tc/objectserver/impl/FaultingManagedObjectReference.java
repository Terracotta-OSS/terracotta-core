/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.object.ObjectID;
import com.tc.objectserver.core.api.ManagedObject;

import gnu.trove.TLinkable;

public class FaultingManagedObjectReference implements ManagedObjectReference {

  private final ObjectID id;
  private boolean        inProgress;
  private boolean        pinned;
  private boolean        processPending;

  public FaultingManagedObjectReference(ObjectID id) {
    this.id = id;
    this.inProgress = true;
    this.pinned = false;
  }

  public boolean isFaultingInProgress() {
    return inProgress;
  }

  public void faultingComplete() {
    this.inProgress = false;
  }

  public boolean getProcessPendingOnRelease() {
    return this.processPending;
  }

  public void setProcessPendingOnRelease(boolean b) {
    this.processPending = b;
  }

  public void setRemoveOnRelease(boolean removeOnRelease) {
    // NOP
  }

  public boolean isRemoveOnRelease() {
    return true;
  }

  public void markReference() {
    // This Object is always referenced.
  }

  public void unmarkReference() {
    // This Object is always referenced.
  }

  public boolean isReferenced() {
    return true;
  }

  public boolean isNew() {
    return false;
  }

  public void pin() {
    this.pinned = true;
  }

  public void unpin() {
    this.pinned = false;
  }

  public boolean isPinned() {
    return this.pinned;
  }

  public ManagedObject getObject() {
    throw new AssertionError("This should never be called");
  }

  public ObjectID getObjectID() {
    return id;
  }

  public void markAccessed() {
    // NOP
  }

  public void clearAccessed() {
    // NOP
  }

  public boolean recentlyAccessed() {
    return true;
  }
  
  public int accessCount(int factor) {
    return 0;
  }

  public TLinkable getNext() {
    // this object should never go into the cache
    return null;
  }

  public TLinkable getPrevious() {
    // this object should never go into the cache
    return null;
  }

  public void setNext(TLinkable linkable) {
    // this object should never go into the cache
    throw new AssertionError("This should never be called");
  }

  public void setPrevious(TLinkable linkable) {
    // this object should never go into the cache
    throw new AssertionError("This should never be called");
  }

  public boolean canEvict() {
    return false;
  }

  public String toString() {
    return "FaultingManagedObjectReference [ " + id + " inProgress : " + inProgress + " pinned : " + pinned
           + " processPending : " + processPending + " ]";
  }

}
