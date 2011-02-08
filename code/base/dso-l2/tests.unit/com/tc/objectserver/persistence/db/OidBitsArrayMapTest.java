/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.db;

import com.sleepycat.je.Cursor;
import com.sleepycat.je.CursorConfig;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.tc.object.ObjectID;
import com.tc.objectserver.persistence.inmemory.NullPersistenceTransactionProvider;
import com.tc.objectserver.storage.api.TCBytesToBytesDatabase;
import com.tc.objectserver.storage.berkeleydb.BerkeleyDBEnvironment;
import com.tc.objectserver.storage.berkeleydb.BerkeleyDBTCBytesBytesDatabase;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;
import com.tc.util.Conversion;
import com.tc.util.OidLongArray;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class OidBitsArrayMapTest extends TCTestCase {
  private BerkeleyDBEnvironment  env;
  private Database               oidDB;
  private TCBytesToBytesDatabase oidTcBytesBytesDB;
  private final int              LongPerDiskUnit = 8;
  private final int              TestSize        = 500;

  public OidBitsArrayMapTest() {
    //
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    boolean paranoid = true;
    env = newDBEnvironment(paranoid);
    env.open();
    oidTcBytesBytesDB = env.getObjectOidStoreDatabase();
    oidDB = ((BerkeleyDBTCBytesBytesDatabase) oidTcBytesBytesDB).getDatabase();
  }

  @Override
  protected void tearDown() throws Exception {
    env.close();
    super.tearDown();
  }

  private BerkeleyDBEnvironment newDBEnvironment(boolean paranoid) throws Exception {
    File dbHome;
    int count = 0;
    do {
      dbHome = new File(this.getTempDirectory(), getClass().getName() + "db" + (++count));
    } while (dbHome.exists());
    dbHome.mkdir();
    assertTrue(dbHome.exists());
    assertTrue(dbHome.isDirectory());
    System.out.println("DB Home: " + dbHome);
    return new BerkeleyDBEnvironment(paranoid, dbHome);
  }

  private List<ObjectID> populateObjectIDList() {
    List<ObjectID> idList = new ArrayList<ObjectID>();
    Random r = new Random(System.currentTimeMillis());
    for (int i = 0; i < TestSize; ++i) {
      idList.add(new ObjectID(r.nextInt(10000) + 1));
    }
    return idList;
  }

  private void verifyObjectIDInList(List<ObjectID> idList, OidBitsArrayMapDiskStoreImpl oids) {
    for (int i = 0; i < idList.size(); ++i) {
      Assert.assertTrue("Not found index=" + oids.oidIndex(idList.get(i)), oids.contains(idList.get(i)));
    }
  }

  private void saveAllToDisk(OidBitsArrayMapDiskStoreImpl oids) throws TCDatabaseException {

    // use another set to avoid ConcurrentModificationException
    Set dupKeySet = new HashSet();
    dupKeySet.addAll(oids.getMap().keySet());
    Iterator i = dupKeySet.iterator();
    while (i.hasNext()) {
      OidLongArray bitsArray = oids.getMap().get(i.next());
      try {
        oids.writeDiskEntry(null, bitsArray);
      } catch (DatabaseException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private void loadAllFromDisk(OidBitsArrayMapDiskStoreImpl oids) {
    oids.getMap().clear();
    Cursor cursor = null;
    try {
      cursor = oidDB.openCursor(null, CursorConfig.READ_COMMITTED);
      DatabaseEntry key = new DatabaseEntry();
      DatabaseEntry value = new DatabaseEntry();
      while (OperationStatus.SUCCESS.equals(cursor.getNext(key, value, LockMode.DEFAULT))) {
        // load its only records indicated by auxKey
        long index = Conversion.bytes2Long(key.getData());
        if (index == (oids.oidIndex(index) + oids.getAuxKey())) {
          index -= oids.getAuxKey();
          OidLongArray bitsArray = new OidLongArray(index, value.getData());
          oids.getMap().put(new Long(index), bitsArray);
        }
      }
      cursor.close();
      cursor = null;
    } catch (DatabaseException e) {
      throw new RuntimeException(e);
    } finally {
      try {
        if (cursor != null) cursor.close();
      } catch (DatabaseException e) {
        throw new RuntimeException(e);
      }
    }

  }

  public void testReadWriteDB() throws Exception {
    List<ObjectID> idList = populateObjectIDList();

    OidBitsArrayMapDiskStoreImpl oids = new OidBitsArrayMapDiskStoreImpl(LongPerDiskUnit, oidTcBytesBytesDB,
                                                                         new NullPersistenceTransactionProvider());

    for (ObjectID id : idList) {
      oids.getAndSet(id, null);
    }

    // write and read back
    saveAllToDisk(oids);
    loadAllFromDisk(oids);
    verifyObjectIDInList(idList, oids);

    // load to a new OidBitsArrayMap
    OidBitsArrayMapDiskStoreImpl secOids = new OidBitsArrayMapDiskStoreImpl(LongPerDiskUnit, oidTcBytesBytesDB,
                                                                            new NullPersistenceTransactionProvider());
    loadAllFromDisk(secOids);
    verifyObjectIDInList(idList, secOids);

    // remove all one by one
    for (ObjectID id : idList) {
      secOids.getAndClr(id, null);
    }

    // verify
    Set keySet = secOids.getMap().keySet();
    Iterator i = keySet.iterator();
    while (i.hasNext()) {
      OidLongArray ary = secOids.getBitsArray(((Long) i.next()));
      Assert.assertTrue(ary.isZero());
    }

    // zero arrays shall be removed from db
    saveAllToDisk(secOids);
    loadAllFromDisk(secOids);
    Assert.assertEquals(0, secOids.getMap().keySet().size());
  }

  public void testReadWriteAuxDB() throws Exception {
    int auxDB = 1;
    List<ObjectID> idList = populateObjectIDList();

    OidBitsArrayMapDiskStoreImpl oids = new OidBitsArrayMapDiskStoreImpl(LongPerDiskUnit, oidTcBytesBytesDB, auxDB,
                                                                         new NullPersistenceTransactionProvider());

    for (ObjectID id : idList) {
      oids.getAndSet(id, null);
    }

    // write and read back
    saveAllToDisk(oids);
    loadAllFromDisk(oids);
    verifyObjectIDInList(idList, oids);

    // load to a new OidBitsArrayMap
    OidBitsArrayMapDiskStoreImpl secOids = new OidBitsArrayMapDiskStoreImpl(LongPerDiskUnit, oidTcBytesBytesDB, auxDB,
                                                                            new NullPersistenceTransactionProvider());
    loadAllFromDisk(secOids);
    verifyObjectIDInList(idList, secOids);

    // remove all one by one
    for (ObjectID id : idList) {
      secOids.getAndClr(id, null);
    }

    // verify
    Set keySet = secOids.getMap().keySet();
    Iterator i = keySet.iterator();
    while (i.hasNext()) {
      OidLongArray ary = secOids.getBitsArray(((Long) i.next()));
      Assert.assertTrue(ary.isZero());
    }

    // zero arrays shall be removed from db
    saveAllToDisk(secOids);
    loadAllFromDisk(secOids);
    Assert.assertEquals(0, secOids.getMap().keySet().size());
  }

  public void testReadWriteDBandAuxDB() throws Exception {
    int auxDB = 1;
    List<ObjectID> idList = populateObjectIDList();
    List<ObjectID> auxList = new ArrayList();
    for (int i = 0; i < idList.size(); ++i) {
      if ((i % 3) == 0) auxList.add(idList.get(i));
    }

    OidBitsArrayMapDiskStoreImpl oids = new OidBitsArrayMapDiskStoreImpl(LongPerDiskUnit, oidTcBytesBytesDB,
                                                                         new NullPersistenceTransactionProvider());
    OidBitsArrayMapDiskStoreImpl oidAux = new OidBitsArrayMapDiskStoreImpl(LongPerDiskUnit, oidTcBytesBytesDB, auxDB,
                                                                           new NullPersistenceTransactionProvider());

    for (ObjectID id : idList) {
      oids.getAndSet(id, null);
    }
    for (ObjectID id : auxList) {
      oidAux.getAndSet(id, null);
    }

    // write and read back
    saveAllToDisk(oids);
    saveAllToDisk(oidAux);
    loadAllFromDisk(oids);
    loadAllFromDisk(oidAux);
    verifyObjectIDInList(idList, oids);
    verifyObjectIDInList(auxList, oidAux);

    // load to a new OidBitsArrayMap
    OidBitsArrayMapDiskStoreImpl secOids = new OidBitsArrayMapDiskStoreImpl(LongPerDiskUnit, oidTcBytesBytesDB,
                                                                            new NullPersistenceTransactionProvider());
    loadAllFromDisk(secOids);
    verifyObjectIDInList(idList, secOids);
    OidBitsArrayMapDiskStoreImpl secAux = new OidBitsArrayMapDiskStoreImpl(LongPerDiskUnit, oidTcBytesBytesDB, auxDB,
                                                                           new NullPersistenceTransactionProvider());
    loadAllFromDisk(secAux);
    verifyObjectIDInList(auxList, secAux);

    // remove all one by one
    for (ObjectID id : idList) {
      secOids.getAndClr(id, null);
    }
    for (ObjectID id : auxList) {
      secAux.getAndClr(id, null);
    }

    // verify
    Set keySet = secOids.getMap().keySet();
    Iterator i = keySet.iterator();
    while (i.hasNext()) {
      OidLongArray ary = secOids.getBitsArray(((Long) i.next()));
      Assert.assertTrue(ary.isZero());
    }
    keySet = secAux.getMap().keySet();
    i = keySet.iterator();
    while (i.hasNext()) {
      OidLongArray ary = secAux.getBitsArray(((Long) i.next()));
      Assert.assertTrue(ary.isZero());
    }

    // zero arrays shall be removed from db
    saveAllToDisk(secOids);
    loadAllFromDisk(secOids);
    Assert.assertEquals(0, secOids.getMap().keySet().size());
    saveAllToDisk(secAux);
    loadAllFromDisk(secAux);
    Assert.assertEquals(0, secAux.getMap().keySet().size());
  }

  public void baseTestReadDiskEntry(int auxDB) throws TCDatabaseException {
    List<ObjectID> idList = populateObjectIDList();
    Set<Long> indexSet = new HashSet<Long>();
    Map<Long, OidLongArray> map = new HashMap<Long, OidLongArray>();

    OidBitsArrayMapDiskStoreImpl oids = new OidBitsArrayMapDiskStoreImpl(LongPerDiskUnit, oidTcBytesBytesDB, auxDB,
                                                                         new NullPersistenceTransactionProvider());

    for (ObjectID id : idList) {
      oids.getAndSet(id, null);
      Assert.assertTrue(oids.contains(id));
      indexSet.add(oids.oidIndex(id));
    }

    saveAllToDisk(oids);

    // read back to memory
    for (Long index : indexSet) {
      try {
        OidLongArray ary = oids.readDiskEntry(null, index);
        Assert.assertEquals((long) index, ary.getKey());
        map.put(index, ary);
      } catch (DatabaseException e) {
        throw new RuntimeException(e);
      }
    }

    // verify
    for (ObjectID id : idList) {
      long index = oids.oidIndex(id);
      int offset = (int) (id.toLong() - index);
      OidLongArray ary = map.get(index);
      Assert.assertTrue("" + id + " index=" + index + " offset=" + offset, ary.isSet(offset));
    }
    // clear bits
    for (ObjectID id : idList) {
      long index = oids.oidIndex(id);
      int offset = (int) (id.toLong() - index);
      OidLongArray ary = map.get(index);
      ary.clrBit(offset);
    }

    for (Long index : map.keySet()) {
      OidLongArray ary = map.get(index);
      Assert.assertTrue(ary.isZero());
    }
  }

  public void testReadDiskEntry() throws Exception {
    baseTestReadDiskEntry(0);
  }

  public void testAuxReadDiskEntry() throws Exception {
    baseTestReadDiskEntry(1);
  }

}
