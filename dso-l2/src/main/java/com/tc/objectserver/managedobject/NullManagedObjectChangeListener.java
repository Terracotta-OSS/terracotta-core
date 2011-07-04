/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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