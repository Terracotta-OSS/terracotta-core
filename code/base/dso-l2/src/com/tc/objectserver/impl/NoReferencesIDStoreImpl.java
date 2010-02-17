/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.object.ObjectID;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.ObjectIDSet;
import com.tc.util.ObjectIDSet.ObjectIDSetType;

public class NoReferencesIDStoreImpl implements NoReferencesIDStore {

  private static final boolean      FAULTING_OPTIMIZATION = TCPropertiesImpl
                                                              .getProperties()
                                                              .getBoolean(
                                                                          TCPropertiesConsts.L2_OBJECTMANAGER_DGC_FAULTING_OPTIMIZATION,
                                                                          true);
  private final NoReferencesIDStore delegate;

  public NoReferencesIDStoreImpl() {
    if (FAULTING_OPTIMIZATION) {
      delegate = new OidSetStore();
    } else  {
      delegate = NoReferencesIDStore.NULL_NO_REFERENCES_ID_STORE;
    }
  }

  public void addToNoReferences(ManagedObject mo) {
    delegate.addToNoReferences(mo);
  }

  public void clearFromNoReferencesStore(ObjectID id) {
    delegate.clearFromNoReferencesStore(id);
  }

  public boolean hasNoReferences(ObjectID id) {
    return delegate.hasNoReferences(id);
  }

  public class OidSetStore implements NoReferencesIDStore {

    private final ObjectIDSet store = new ObjectIDSet(ObjectIDSetType.BITSET_BASED_SET);

    public void addToNoReferences(ManagedObject mo) {
      if (mo.getManagedObjectState().hasNoReferences()) {
        this.store.add(mo.getID());
      }
    }

    public void clearFromNoReferencesStore(ObjectID id) {
      store.remove(id);
    }

    public boolean hasNoReferences(ObjectID id) {
      return store.contains(id);
    }

  }

}
