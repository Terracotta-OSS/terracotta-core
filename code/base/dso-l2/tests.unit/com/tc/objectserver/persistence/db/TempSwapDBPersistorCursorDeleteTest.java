/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.persistence.db;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ObjectID;
import com.tc.object.TestDNACursor;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.core.api.ManagedObjectState;
import com.tc.objectserver.core.impl.TestManagedObject;
import com.tc.objectserver.managedobject.ManagedObjectStateFactory;
import com.tc.objectserver.managedobject.MapManagedObjectState;
import com.tc.objectserver.managedobject.NullManagedObjectChangeListenerProvider;
import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.objectserver.storage.api.PersistenceTransactionProvider;
import com.tc.objectserver.storage.berkeleydb.BerkeleyDBEnvironment;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.test.TCTestCase;
import com.tc.util.ObjectIDSet;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;

public class TempSwapDBPersistorCursorDeleteTest extends TCTestCase {
  public void testBasic() throws Exception {
    TCLogger logger = TCLogging.getLogger(getClass());
    BerkeleyDBEnvironment dbEnv = newDBEnvironment(false);
    SerializationAdapterFactory saf = new CustomSerializationAdapterFactory();
    TempSwapDBPersistorImpl persistor = new TempSwapDBPersistorImpl(logger, dbEnv, saf);
    PersistenceTransactionProvider ptp = dbEnv.getPersistenceTransactionProvider();

    ManagedObjectStateFactory.createInstance(new NullManagedObjectChangeListenerProvider(), persistor);

    final ObjectID id = new ObjectID(0);
    final MapManagedObjectState state = (MapManagedObjectState) ManagedObjectStateFactory.getInstance()
        .createState(id, ObjectID.NULL_ID, "java.util.HashMap", "System.loader", new TestDNACursor());
    final ManagedObject mo = new TestPersistentStateManagedObject(id, new ArrayList<ObjectID>(),
                                                                  ManagedObjectState.MAP_TYPE, state);
    persistor.getManagedObjectPersistor().addNewObject(mo);

    final TCPersistableMap sMap = (TCPersistableMap) state.getPersistentCollection();
    int entries = TCPropertiesImpl.getProperties().getInt(TCPropertiesConsts.L2_OBJECTMANAGER_DELETEBATCHSIZE) - 1;
    addToMap(sMap, entries);
    PersistenceTransaction tx = ptp.newTransaction();
    persistor.getCollectionsPersistor().saveCollections(tx, state);
    tx.commit();

    System.out.println("started deleting collections");
    ObjectIDSet set = new ObjectIDSet();
    set.add(id);
    persistor.getManagedObjectPersistor().deleteAllObjects(set);
  }

  private void addToMap(final Map map, final int numOfEntries) {
    for (int i = 50; i < numOfEntries + 50; i++) {
      map.put(new ObjectID(i), new Integer(i));
    }
  }

  private BerkeleyDBEnvironment newDBEnvironment(final boolean paranoid) throws Exception {
    File dbHome;
    int count = 0;
    do {
      dbHome = new File(getTempDirectory(), getClass().getName() + "db" + (++count));
    } while (dbHome.exists());
    dbHome.mkdir();
    assertTrue(dbHome.exists());
    assertTrue(dbHome.isDirectory());
    System.out.println("DB Home: " + dbHome);
    return new BerkeleyDBEnvironment(paranoid, dbHome);
  }

  private class TestPersistentStateManagedObject extends TestManagedObject {

    private final ManagedObjectState state;

    public TestPersistentStateManagedObject(final ObjectID id, final ArrayList<ObjectID> references,
                                            final byte stateType, final ManagedObjectState state) {
      super(id, references);
      this.state = state;
    }

    @Override
    public boolean isNew() {
      return true;
    }

    @Override
    public ManagedObjectState getManagedObjectState() {
      return this.state;
    }

    @Override
    public boolean equals(final Object other) {
      if (!(other instanceof TestPersistentStateManagedObject)) { return false; }
      final TestPersistentStateManagedObject o = (TestPersistentStateManagedObject) other;
      return getID().toLong() == o.getID().toLong();
    }

    @Override
    public int hashCode() {
      return (int) getID().toLong();
    }
  }
}
