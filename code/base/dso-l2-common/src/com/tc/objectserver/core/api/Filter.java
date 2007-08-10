/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.core.api;

import com.tc.object.ObjectID;

public interface Filter {
  public boolean shouldVisit(ObjectID referencedObject);
}
