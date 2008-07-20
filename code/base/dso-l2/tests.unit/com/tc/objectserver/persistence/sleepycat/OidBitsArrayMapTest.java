/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;

import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseException;
import com.tc.object.ObjectID;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;
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
  private DBEnvironment env;
  private Database      oidDB;
  private final int     LongPerDiskUnit = 8;
  private final int     TestSize        = 500;

  public OidBitsArrayMapTest() {
    //
  }

  protected void setUp() throws Exception {
    super.setUp();
    boolean paranoid = true;
    env = newDBEnvironment(paranoid);
    env.open();
    oidDB = env.getObjectOidStoreDatabase();
  }

  protected void tearDown() throws Exception {
    env.close();
    super.tearDown();
  }

  private DBEnvironment newDBEnvironment(boolean paranoid) throws Exception {
    File dbHome;
    int count = 0;
    do {
      dbHome = new File(this.getTempDirectory(), getClass().getName() + "db" + (++count));
    } while (dbHome.exists());
    dbHome.mkdir();
    assertTrue(dbHome.exists());
    assertTrue(dbHome.isDirectory());
    System.out.println("DB Home: " + dbHome);
    return new DBEnvironment(paranoid, dbHome);
  }

  private List<ObjectID> populateObjectIDList() {
    List<ObjectID> idList = new ArrayList<ObjectID>();
    Random r = new Random(System.currentTimeMillis());
    for (int i = 0; i < TestSize; ++i) {
      idList.add(new ObjectID(r.nextInt(10000) + 1));
    }
    return idList;
  }

  private void verifyObjectIDInList(List<ObjectID> idList, OidBitsArrayMapImpl oids) {
    for (int i = 0; i < idList.size(); ++i) {
      Assert.assertTrue("Not found index=" + oids.oidIndex(idList.get(i)), oids.contains(idList.get(i)));
    }
  }

  public void testReadWriteDB() {
    List<ObjectID> idList = populateObjectIDList();

    OidBitsArrayMapImpl oids = new OidBitsArrayMapImpl(LongPerDiskUnit, oidDB);

    for (ObjectID id : idList) {
      oids.getAndSet(id);
    }

    // write and read back
    oids.saveAllToDisk();
    oids.loadAllFromDisk();
    verifyObjectIDInList(idList, oids);

    // load to a new OidBitsArrayMap
    OidBitsArrayMapImpl secOids = new OidBitsArrayMapImpl(LongPerDiskUnit, oidDB);
    secOids.loadAllFromDisk();
    verifyObjectIDInList(idList, secOids);

    // remove all one by one
    for (ObjectID id : idList) {
      secOids.getAndClr(id);
    }

    // verify
    Set keySet = secOids.getMapKeySet();
    Iterator i = keySet.iterator();
    while (i.hasNext()) {
      OidLongArray ary = secOids.getBitsArray(((Long) i.next()));
      Assert.assertTrue(ary.isZero());
    }

    // zero arrays shall be removed from db
    secOids.saveAllToDisk();
    secOids.loadAllFromDisk();
    Assert.assertEquals(0, secOids.getMapKeySet().size());
  }

  public void testReadWriteAuxDB() {
    int auxDB = 1;
    List<ObjectID> idList = populateObjectIDList();

    OidBitsArrayMapImpl oids = new OidBitsArrayMapImpl(LongPerDiskUnit, oidDB, auxDB);

    for (ObjectID id : idList) {
      oids.getAndSet(id);
    }

    // write and read back
    oids.saveAllToDisk();
    oids.loadAllFromDisk();
    verifyObjectIDInList(idList, oids);

    // load to a new OidBitsArrayMap
    OidBitsArrayMapImpl secOids = new OidBitsArrayMapImpl(LongPerDiskUnit, oidDB, auxDB);
    secOids.loadAllFromDisk();
    verifyObjectIDInList(idList, secOids);

    // remove all one by one
    for (ObjectID id : idList) {
      secOids.getAndClr(id);
    }

    // verify
    Set keySet = secOids.getMapKeySet();
    Iterator i = keySet.iterator();
    while (i.hasNext()) {
      OidLongArray ary = secOids.getBitsArray(((Long) i.next()));
      Assert.assertTrue(ary.isZero());
    }

    // zero arrays shall be removed from db
    secOids.saveAllToDisk();
    secOids.loadAllFromDisk();
    Assert.assertEquals(0, secOids.getMapKeySet().size());
  }

  public void testReadWriteDBandAuxDB() {
    int auxDB = 1;
    List<ObjectID> idList = populateObjectIDList();
    List<ObjectID> auxList = new ArrayList();
    for (int i = 0; i < idList.size(); ++i) {
      if ((i % 3) == 0) auxList.add(idList.get(i));
    }

    OidBitsArrayMapImpl oids = new OidBitsArrayMapImpl(LongPerDiskUnit, oidDB);
    OidBitsArrayMapImpl oidAux = new OidBitsArrayMapImpl(LongPerDiskUnit, oidDB, auxDB);

    for (ObjectID id : idList) {
      oids.getAndSet(id);
    }
    for (ObjectID id : auxList) {
      oidAux.getAndSet(id);
    }

    // write and read back
    oids.saveAllToDisk();
    oidAux.saveAllToDisk();
    oids.loadAllFromDisk();
    oidAux.loadAllFromDisk();
    verifyObjectIDInList(idList, oids);
    verifyObjectIDInList(auxList, oidAux);

    // load to a new OidBitsArrayMap
    OidBitsArrayMapImpl secOids = new OidBitsArrayMapImpl(LongPerDiskUnit, oidDB);
    secOids.loadAllFromDisk();
    verifyObjectIDInList(idList, secOids);
    OidBitsArrayMapImpl secAux = new OidBitsArrayMapImpl(LongPerDiskUnit, oidDB, auxDB);
    secAux.loadAllFromDisk();
    verifyObjectIDInList(auxList, secAux);

    // remove all one by one
    for (ObjectID id : idList) {
      secOids.getAndClr(id);
    }
    for (ObjectID id : auxList) {
      secAux.getAndClr(id);
    }

    // verify
    Set keySet = secOids.getMapKeySet();
    Iterator i = keySet.iterator();
    while (i.hasNext()) {
      OidLongArray ary = secOids.getBitsArray(((Long) i.next()));
      Assert.assertTrue(ary.isZero());
    }
    keySet = secAux.getMapKeySet();
    i = keySet.iterator();
    while (i.hasNext()) {
      OidLongArray ary = secAux.getBitsArray(((Long) i.next()));
      Assert.assertTrue(ary.isZero());
    }

    // zero arrays shall be removed from db
    secOids.saveAllToDisk();
    secOids.loadAllFromDisk();
    Assert.assertEquals(0, secOids.getMapKeySet().size());
    secAux.saveAllToDisk();
    secAux.loadAllFromDisk();
    Assert.assertEquals(0, secAux.getMapKeySet().size());
  }

  public void baseTestReadDiskEntry(int auxDB) {
    List<ObjectID> idList = populateObjectIDList();
    Set<Long> indexSet = new HashSet<Long>();
    Map<Long, OidLongArray> map = new HashMap<Long, OidLongArray>();

    OidBitsArrayMapImpl oids = new OidBitsArrayMapImpl(LongPerDiskUnit, oidDB, auxDB);

    for (ObjectID id : idList) {
      oids.getAndSet(id);
      Assert.assertTrue(oids.contains(id));
      indexSet.add(oids.oidIndex(id));
    }

    oids.saveAllToDisk();

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

  public void testReadDiskEntry() {
    baseTestReadDiskEntry(0);
  }

  public void testAuxReadDiskEntry() {
    baseTestReadDiskEntry(1);
  }

}
