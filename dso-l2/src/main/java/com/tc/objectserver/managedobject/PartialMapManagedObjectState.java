/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.object.ObjectID;
import com.tc.objectserver.persistence.PersistentObjectFactory;

import java.io.IOException;
import java.io.ObjectInput;

/**
 * This class represents Maps that can handle partial collections in the L1 side. Currently supported classses are
 * HashMap, LinkedHashMap, Hashtable and Properties. This class should eventually go away once we support partial
 * collections to all maps.
 */
public class PartialMapManagedObjectState extends MapManagedObjectState {

  protected PartialMapManagedObjectState(final long classID, ObjectID id, PersistentObjectFactory factory) {
    super(classID, id, factory);
  }

  protected PartialMapManagedObjectState(final ObjectInput in, PersistentObjectFactory factory) throws IOException {
    super(in, factory);
  }

  @Override
  public void addObjectReferencesTo(final ManagedObjectTraverser traverser) {
    traverser.addRequiredObjectIDs(getObjectReferencesFrom(this.references.keySet()));
    traverser.addReachableObjectIDs(getObjectReferencesFrom(this.references.values()));
  }

  @Override
  protected void addBackReferenceForValue(final ApplyTransactionInfo includeIDs, final ObjectID value, final ObjectID map) {
    // Not adding to the backreference so the we dont force the server to do a prefetch on apply
  }

  @Override
  public byte getType() {
    return PARTIAL_MAP_TYPE;
  }

  static MapManagedObjectState readFrom(final ObjectInput in, PersistentObjectFactory factory) throws IOException, ClassNotFoundException {
    return new PartialMapManagedObjectState(in, factory);
  }
}
