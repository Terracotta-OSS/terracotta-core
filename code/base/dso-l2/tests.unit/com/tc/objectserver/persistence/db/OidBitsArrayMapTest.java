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
import com.tc.objectserver.storage.api.PersistenceTransactionProvider;
import com.tc.objectserver.storage.api.TCBytesToBytesDatabase;
import com.tc.objectserver.storage.berkeleydb.BerkeleyDBEnvironment;
import com.tc.objectserver.storage.berkeleydb.BerkeleyDBTCBytesBytesDatabase;
import com.tc.objectserver.storage.derby.DerbyDBEnvironment;
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
  private static final int       LongPerDiskUnit = 8;
  private static final int       TestSize        = 500;

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

  private DerbyDBEnvironment newDerbyDBEnvironment(boolean paranoid) throws Exception {
    File dbHome;
    int count = 0;
    do {
      dbHome = new File(this.getTempDirectory(), getClass().getName() + "db" + (++count));
    } while (dbHome.exists());
    dbHome.mkdir();
    assertTrue(dbHome.exists());
    assertTrue(dbHome.isDirectory());
    System.out.println("DB Home: " + dbHome);
    return new DerbyDBEnvironment(paranoid, dbHome);
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
          oids.getMap().put(Long.valueOf(index), bitsArray);
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

  private void loadFromDiskStore(List<ObjectID> idList, OidBitsArrayMapDiskStoreImpl oids) {
    for (int i = 0; i < idList.size(); ++i) {
      try {
        if (null != oids.readDiskEntry(null, idList.get(i).toLong())) {
          oids.getAndSet(idList.get(i), null);
        }
      } catch (TCDatabaseException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private void verifyOnDiskEntry(List<ObjectID> idList, OidBitsArrayMapDiskStoreImpl oids) {
    int totalBits = oids.getBitsArray(idList.get(0).toLong()).totalBits();
    for (int i = 0; i < idList.size(); ++i) {
      long aryIndex = oids.oidIndex(idList.get(i));
      ObjectID oid = new ObjectID(aryIndex / totalBits);
      Assert.assertTrue("Not found index " + aryIndex, oids.getOnDiskEntries().contains(oid));
    }
  }

  public void testOnDiskEntries() throws Exception {
    List<ObjectID> idList = populateObjectIDList();

    OidBitsArrayMapDiskStoreImpl oids = new OidBitsArrayMapDiskStoreImpl(LongPerDiskUnit, oidTcBytesBytesDB,
                                                                         new NullPersistenceTransactionProvider());

    for (ObjectID id : idList) {
      oids.getAndSet(id, null);
    }

    // write and read back
    saveAllToDisk(oids);
    oids.getOnDiskEntries().clear();
    loadFromDiskStore(idList, oids);
    verifyOnDiskEntry(idList, oids);

    // load to a new OidBitsArrayMap
    OidBitsArrayMapDiskStoreImpl secOids = new OidBitsArrayMapDiskStoreImpl(LongPerDiskUnit, oidTcBytesBytesDB,
                                                                            new NullPersistenceTransactionProvider());
    loadFromDiskStore(idList, secOids);
    verifyOnDiskEntry(idList, secOids);

    secOids.flushToDisk(null);

    // verify in memory onDiskEntries shall drop all mappings
    Assert.assertEquals(0, secOids.getOnDiskEntries().size());

  }

  public void testOnDiskEntriesBug() throws Exception {
    DerbyDBEnvironment dbEnvironment = newDerbyDBEnvironment(true);
    dbEnvironment.open();
    PersistenceTransactionProvider ptp = dbEnvironment.getPersistenceTransactionProvider();
    TCBytesToBytesDatabase tcBytesToBytesDatabase = dbEnvironment.getObjectOidStoreDatabase();

    List<ObjectID> idList = new ArrayList<ObjectID>();
    final ObjectID oid512 = new ObjectID(512);
    final ObjectID oid512_512 = new ObjectID(512 * 512);
    final ObjectID oid512_512_1 = new ObjectID(512 * 512 + 1);

    idList.add(oid512);
    idList.add(oid512_512);

    OidBitsArrayMapDiskStoreImpl oids = new OidBitsArrayMapDiskStoreImpl(LongPerDiskUnit, tcBytesToBytesDatabase, ptp);

    for (ObjectID id : idList) {
      oids.getAndSet(id, ptp.newTransaction());
    }

    // write and read back
    oids.flushToDisk(ptp.newTransaction());

    oids.readDiskEntry(ptp.newTransaction(), oid512.toLong());
    oids.readDiskEntry(ptp.newTransaction(), oid512_512.toLong());

    oids.getAndSet(oid512_512_1, ptp.newTransaction());
    idList.add(oid512_512_1);

    oids.getAndClr(oid512, ptp.newTransaction());
    idList.remove(oid512);

    oids.flushToDisk(ptp.newTransaction());

    dbEnvironment.close();
  }
}
