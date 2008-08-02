/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.dgc.impl;

import com.tc.object.ObjectID;
import com.tc.objectserver.core.api.Filter;

import java.util.Set;

final class SelectiveFilter implements Filter {
  private final Set keys;

  public SelectiveFilter(Set keys) {
    this.keys = keys;
  }

  public boolean shouldVisit(ObjectID referencedObject) {
    return keys.contains(referencedObject);
  }
}