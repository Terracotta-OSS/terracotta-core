/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import java.lang.ref.ReferenceQueue;

/**
 * @author gkeim
 */
public class LoggingWeakObjectReference extends WeakObjectReference {
  private final String type;

  public LoggingWeakObjectReference(ObjectID id, Object referent, ReferenceQueue q) {
    super(id, referent, q);
    type = referent.getClass().getName();
  }

  public String getObjectType() {
    return type;
  }
}