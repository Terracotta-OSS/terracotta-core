/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.dgc.impl;

import com.tc.object.ObjectID;
import com.tc.util.BitSetObjectIDSet;

import java.util.Set;

public class NewReferenceCollector implements ChangeCollector {

  private final Set<ObjectID> newReferences = new BitSetObjectIDSet();

  @Override
  public synchronized void changed(ObjectID changedObject, ObjectID oldReference, ObjectID newReference) {
    this.newReferences.add(newReference);
  }

  @Override
  public synchronized Set<ObjectID> addNewReferencesTo(Set<ObjectID> set) {
    set.addAll(this.newReferences);
    return set;
  }

}