/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.object.ObjectID;

/**
 * Interface for listening for changes to managed objects
 */
public interface ManagedObjectChangeListener {

  public void changed(ObjectID changedObject, ObjectID oldReference, ObjectID newReference);

}