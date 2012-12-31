/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.object.ObjectID;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.StripedObjectIDSet;

public class NoReferencesIDStoreImpl implements NoReferencesIDStore {

  private static final boolean      FAULTING_OPTIMIZATION = TCPropertiesImpl
                                                              .getProperties()
                                                              .getBoolean(
                                                                          TCPropertiesConsts.L2_OBJECTMANAGER_DGC_FAULTING_OPTIMIZATION,
                                                                          true);
  private final NoReferencesIDStore delegate;

  public NoReferencesIDStoreImpl(boolean isGcEnabled) {
    if (FAULTING_OPTIMIZATION && isGcEnabled) {
      this.delegate = new OidSetStore();
    } else {
      this.delegate = NoReferencesIDStore.NULL_NO_REFERENCES_ID_STORE;
    }
  }

  public void addToNoReferences(final ManagedObject mo) {
    this.delegate.addToNoReferences(mo);
  }

  public void clearFromNoReferencesStore(final ObjectID id) {
    this.delegate.clearFromNoReferencesStore(id);
  }

  public boolean hasNoReferences(final ObjectID id) {
    return this.delegate.hasNoReferences(id);
  }

  private static class OidSetStore implements NoReferencesIDStore {
    private final StripedObjectIDSet store = new StripedObjectIDSet();

    public void addToNoReferences(final ManagedObject mo) {
      if (mo.getManagedObjectState().hasNoReferences()) {
        this.store.add(mo.getID());
      }
    }

    public void clearFromNoReferencesStore(final ObjectID id) {
      this.store.remove(id);
    }

    public boolean hasNoReferences(final ObjectID id) {
      return this.store.contains(id);
    }

  }

}
