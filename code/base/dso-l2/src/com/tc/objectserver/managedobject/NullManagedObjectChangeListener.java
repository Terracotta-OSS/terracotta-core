/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.object.ObjectID;

public class NullManagedObjectChangeListener implements ManagedObjectChangeListener {

  public NullManagedObjectChangeListener() {
    super();
  }

  public void changed(ObjectID changedObject, ObjectID oldReference, ObjectID newReference) {
    // null
  }

}