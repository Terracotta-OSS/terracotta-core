/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

/**
 * @author steve
 */
public class WeakObjectReference extends WeakReference {
  private final ObjectID id;

  public WeakObjectReference(ObjectID id, Object referent, ReferenceQueue q) {
    super(referent, q);
    if (referent == null) { throw new AssertionError("null reference"); }
    this.id = id;
  }

  public ObjectID getObjectID() {
    return id;
  }

  public String getObjectType() {
    return null;
  }
}