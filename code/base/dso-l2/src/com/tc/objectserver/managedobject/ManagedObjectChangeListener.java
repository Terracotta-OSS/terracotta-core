/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.object.ObjectID;

/**
 * Interface for listening for changes to managed objects
 */
public interface ManagedObjectChangeListener {

  public void changed(ObjectID changedObject, ObjectID oldReference, ObjectID newReference);

}