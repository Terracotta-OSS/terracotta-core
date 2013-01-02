/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.dgc.impl;

import com.tc.object.ObjectID;

import java.util.Set;

public final class NullChangeCollector implements ChangeCollector {
  @Override
  public void changed(ObjectID changedObject, ObjectID oldReference, ObjectID newReference) {
    return;
  }

  @Override
  public Set addNewReferencesTo(Set set) {
    return set;
  }
}