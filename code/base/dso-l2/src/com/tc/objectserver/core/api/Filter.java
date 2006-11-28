/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.objectserver.core.api;

import com.tc.object.ObjectID;

public interface Filter {
  public boolean shouldVisit(ObjectID referencedObject);
}
