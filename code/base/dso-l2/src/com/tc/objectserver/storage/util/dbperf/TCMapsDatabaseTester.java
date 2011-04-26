/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.util.dbperf;

import com.tc.objectserver.persistence.db.TCCollectionsSerializer;
import com.tc.objectserver.persistence.db.TCCollectionsSerializerImpl;
import com.tc.objectserver.persistence.db.TCDatabaseException;
import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.objectserver.storage.api.TCMapsDatabase;
import com.tc.util.Conversion;

import java.io.IOException;
import java.util.HashMap;

public class TCMapsDatabaseTester extends AbstractTCDatabaseTester {
  private static final int              MAP_SIZE = 100;

  private final TCMapsDatabase          mapsDB;
  private final TCCollectionsSerializer serializer;

  public TCMapsDatabaseTester(TCMapsDatabase mapsDB) {
    this.mapsDB = mapsDB;
    this.serializer = new TCCollectionsSerializerImpl();
  }

  @Override
  protected void insertInternal(PersistenceTransaction tx) throws TCDatabaseException, IOException {
    long objectId = nextNewObjectId();
    long mapId = objectId % MAP_SIZE;
    mapsDB.insert(tx, mapId, Conversion.long2Bytes(objectId), newValue(), serializer);
  }

  @Override
  protected void updateInternal(PersistenceTransaction tx) throws TCDatabaseException, IOException {
    long objectId = nextExistentObjectId();
    long mapId = objectId % MAP_SIZE;
    mapsDB.update(tx, mapId, Conversion.long2Bytes(objectId), newValue(), serializer);
  }

  @Override
  protected void putInternal(PersistenceTransaction tx) throws TCDatabaseException, IOException {
    long objectId = 0;
    if (random.nextBoolean() && getNumberOfObjects() > 100) {
      objectId = nextExistentObjectId();
    } else {
      objectId = nextNewObjectId();
    }
    long mapId = objectId % MAP_SIZE;
    mapsDB.put(tx, mapId, Conversion.long2Bytes(objectId), newValue(), serializer);
  }

  @Override
  protected void deleteInternal(PersistenceTransaction tx) throws TCDatabaseException, IOException {
    long objectId = nextOldObjectId();
    long mapId = objectId % MAP_SIZE;
    mapsDB.delete(tx, mapId, Conversion.long2Bytes(objectId), serializer);
  }

  @Override
  protected void getInternal(PersistenceTransaction tx) throws TCDatabaseException {
    long objectId = nextExistentObjectId();
    long mapId = objectId % MAP_SIZE;
    mapsDB.loadMap(tx, mapId, new HashMap(), serializer);
  }
}
