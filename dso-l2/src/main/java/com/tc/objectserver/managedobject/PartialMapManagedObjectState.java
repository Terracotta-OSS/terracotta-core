/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.object.ObjectID;

import java.io.IOException;
import java.io.ObjectInput;
import java.util.Map;

/**
 * This class represents Maps that can handle partial collections in the L1 side. Currently supported classses are
 * HashMap, LinkedHashMap, Hashtable and Properties. This class should eventually go away once we support partial
 * collections to all maps.
 */
public class PartialMapManagedObjectState extends MapManagedObjectState {

  protected PartialMapManagedObjectState(final long classID, final Map map) {
    super(classID, map);
  }

  protected PartialMapManagedObjectState(final ObjectInput in) throws IOException {
    super(in);
  }

  @Override
  public void addObjectReferencesTo(final ManagedObjectTraverser traverser) {
    traverser.addRequiredObjectIDs(getObjectReferencesFrom(this.references.keySet()));
    traverser.addReachableObjectIDs(getObjectReferencesFrom(this.references.values()));
  }

  @Override
  protected void addBackReferenceForValue(final ApplyTransactionInfo includeIDs, final ObjectID value, final ObjectID map) {
    // Not adding to the backreference so the we dont force the server to do a prefetch on apply
    return;
  }

  @Override
  public byte getType() {
    return PARTIAL_MAP_TYPE;
  }

  static MapManagedObjectState readFrom(final ObjectInput in) throws IOException, ClassNotFoundException {
    if (false) {
      // This is added to make the compiler happy. For some reason if I have readFrom() method throw
      // ClassNotFoundException in LinkedHashMapManagedObjectState, it shows as an error !!
      throw new ClassNotFoundException();
    }
    return new PartialMapManagedObjectState(in);
  }
}
