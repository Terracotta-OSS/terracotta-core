/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */

package com.tc.objectserver.impl;

import com.tc.object.ObjectID;
import com.tc.objectserver.core.api.ManagedObject;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 * @author mscott
 */
public class DeleteReference implements ManagedObjectReference {
  
  private final ObjectID id;
  private final AtomicBoolean marked = new AtomicBoolean(true);

  public DeleteReference(ObjectID id) {
    this.id = id;
  }
  
  @Override
  public ObjectID getObjectID() {
    return id;
  }

  @Override
  public void setRemoveOnRelease(boolean removeOnRelease) {

  }

  @Override
  public boolean isRemoveOnRelease() {
    return true;
  }

  @Override
  public boolean markReference() {
    return false;
  }

  @Override
  public boolean unmarkReference() {
    return marked.compareAndSet(true, false);
  }

  @Override
  public boolean isReferenced() {
    return true;
  }

  @Override
  public boolean isNew() {
    return false;
  }

  @Override
  public ManagedObject getObject() {
    return null;
  }
  
}
