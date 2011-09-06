/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.dgc.impl;

import com.tc.object.ObjectID;
import com.tc.util.ObjectIDSet;

import java.util.Set;

public class NewReferenceCollector implements ChangeCollector {

  private final Set newReferences = new ObjectIDSet();

  public synchronized void changed(ObjectID changedObject, ObjectID oldReference, ObjectID newReference) {
    this.newReferences.add(newReference);
  }

  public synchronized Set addNewReferencesTo(Set set) {
    set.addAll(this.newReferences);
    return set;
  }

}