/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.core.api;

import com.tc.object.ObjectID;

public interface Filter {
  public boolean shouldVisit(ObjectID referencedObject);
}
