/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.object.ObjectID;
import com.tc.objectserver.core.api.ManagedObject;

import gnu.trove.TLinkable;

public class FaultingManagedObjectReference implements ManagedObjectReference {

  private final ObjectID   id;
  private volatile boolean inProgress;

  public FaultingManagedObjectReference(final ObjectID id) {
    this.id = id;
    this.inProgress = true;
  }

  public boolean isFaultingInProgress() {
    return this.inProgress;
  }

  public void faultingFailed() {
    this.inProgress = false;
  }

  public void setRemoveOnRelease(final boolean removeOnRelease) {
    // NOP
  }

  public boolean isRemoveOnRelease() {
    return true;
  }

  public boolean markReference() {
    // This Object is always referenced.
    return false;
  }

  public boolean unmarkReference() {
    // This Object is always referenced.
    return false;
  }

  public boolean isReferenced() {
    return this.inProgress;
  }

  public boolean isNew() {
    return false;
  }

  public ManagedObject getObject() {
    throw new AssertionError("This should never be called");
  }

  public ObjectID getObjectID() {
    return this.id;
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

  public int accessCount(final int factor) {
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

  public void setNext(final TLinkable linkable) {
    // this object should never go into the cache
    throw new AssertionError("This should never be called");
  }

  public void setPrevious(final TLinkable linkable) {
    // this object should never go into the cache
    throw new AssertionError("This should never be called");
  }

  public boolean canEvict() {
    return false;
  }

  public boolean isCacheManaged() {
    return false;
  }

  @Override
  public String toString() {
    return "FaultingManagedObjectReference [ " + this.id + " inProgress : " + this.inProgress + " ]";
  }
}
