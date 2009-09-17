/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.object.ObjectID;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.persistence.sleepycat.OidBitsArrayMap;
import com.tc.objectserver.persistence.sleepycat.OidBitsArrayMapInMemoryImpl;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.ObjectIDSet;

public class NoReferencesIDStoreImpl implements NoReferencesIDStore {

  private static final int          FAULTING_OPTIMIZATION = TCPropertiesImpl
                                                              .getProperties()
                                                              .getInt(
                                                                      TCPropertiesConsts.L2_OBJECTMANAGER_DGC_FAULTING_OPTIMIZATION);
  private final NoReferencesIDStore delegate;

  public NoReferencesIDStoreImpl() {
    if (FAULTING_OPTIMIZATION == 0) {
      delegate = NoReferencesIDStore.NULL_NO_REFERENCES_ID_STORE;
    } else if (FAULTING_OPTIMIZATION == 1) {
      delegate = new OidSetStore();
    } else if (FAULTING_OPTIMIZATION == 2) {
      delegate = new OidBitsStore();
    } else {
      throw new AssertionError("Incorrect faulting optimization property: " + FAULTING_OPTIMIZATION
                               + ", Please choose: \n" + "0 - disable faulting optimization \n"
                               + "1 - enabled with standard implementation (continous oids) \n"
                               + "2 = enabled with compressed implementation (spare oids) \n");
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

    private ObjectIDSet store = new ObjectIDSet();

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

  private static class OidBitsStore implements NoReferencesIDStore {

    private OidBitsArrayMap store = new OidBitsArrayMapInMemoryImpl(8);

    public void addToNoReferences(ManagedObject mo) {
      if (mo.getManagedObjectState().hasNoReferences()) {
        this.store.getAndSet(mo.getID());
      }
    }

    public void clearFromNoReferencesStore(ObjectID id) {
      store.getAndClr(id);
    }

    public boolean hasNoReferences(ObjectID id) {
      return store.contains(id);
    }

  }

}
