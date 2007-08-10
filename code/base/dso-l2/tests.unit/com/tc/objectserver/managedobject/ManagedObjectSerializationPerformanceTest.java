/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.sleepycat.bind.EntryBinding;
import com.sleepycat.bind.serial.SerialBinding;
import com.sleepycat.bind.serial.StoredClassCatalog;
import com.sleepycat.je.DatabaseEntry;
import com.tc.logging.NullTCLogger;
import com.tc.object.ObjectID;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.ObjectInstanceMonitor;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.core.api.TestDNA;
import com.tc.objectserver.core.api.TestDNACursor;
import com.tc.objectserver.impl.ObjectInstanceMonitorImpl;
import com.tc.objectserver.persistence.api.ManagedObjectPersistor;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.objectserver.persistence.sleepycat.CustomSerializationAdapterFactory;
import com.tc.objectserver.persistence.sleepycat.DBEnvironment;
import com.tc.objectserver.persistence.sleepycat.SerializationAdapter;
import com.tc.objectserver.persistence.sleepycat.SerializationAdapterFactory;
import com.tc.objectserver.persistence.sleepycat.SleepycatPersistor;
import com.tc.objectserver.persistence.sleepycat.SleepycatSerializationAdapterFactory;
import com.tc.test.TCTestCase;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class ManagedObjectSerializationPerformanceTest extends TCTestCase {

  private StoredClassCatalog    classCatalog;
  private int                   fieldSetCount = 10;
  private SleepycatPersistor    sleepycatSerializerPersistor;
  private SleepycatPersistor    customSerializerPersistor;

  private boolean               paranoid;
  private DBEnvironment         sleepycatSerializerEnvironment;
  private DBEnvironment         customSerializerEnvironment;
  private Set                   environments  = new HashSet();
  private ObjectInstanceMonitor imo;

  public void setUp() throws Exception {

    paranoid = false;
    imo = new ObjectInstanceMonitorImpl();

    // ManagedObjectChangeListenerProvider listenerProvider = new NullManagedObjectChangeListenerProvider();

    // set up sleepycat serializer persistor
    sleepycatSerializerEnvironment = newEnvironment();
    SerializationAdapterFactory saf = new SleepycatSerializationAdapterFactory();
    sleepycatSerializerPersistor = new SleepycatPersistor(new NullTCLogger(), sleepycatSerializerEnvironment, saf);
    classCatalog = (StoredClassCatalog) sleepycatSerializerEnvironment.getClassCatalogWrapper().getClassCatalog();

    ManagedObjectStateFactory.disableSingleton(true);
    ManagedObjectStateFactory.createInstance(new NullManagedObjectChangeListenerProvider(),
                                             sleepycatSerializerPersistor);

    // set up custom serializer persistor
    customSerializerEnvironment = newEnvironment();

    CustomSerializationAdapterFactory customSaf = new CustomSerializationAdapterFactory();
    customSerializerPersistor = new SleepycatPersistor(new NullTCLogger(), customSerializerEnvironment, customSaf);
  }

  public void tearDown() throws Exception {
    for (Iterator i = environments.iterator(); i.hasNext();) {
      ((DBEnvironment) i.next()).close();
    }
  }

  public void testStoreAndLoad() throws Exception {
    ManagedObjectImpl customSerializerMo = newManagedObject();
    ManagedObjectImpl sleepycatSerializerMo = newManagedObject();

    int iterations = 100;
    int cycles = 5;

    List sleepycatStoreStats = new ArrayList(cycles);
    List sleepycatLoadStats = new ArrayList(cycles);
    List tcStoreStats = new ArrayList(cycles);
    List tcLoadStats = new ArrayList(cycles);
    for (int i = 0; i < cycles; i++) {
      Stats scStoreStat = new Stats(), scLoadStat = new Stats();
      sleepycatStoreStats.add(scStoreStat);
      sleepycatLoadStats.add(scLoadStat);

      Stats tcStoreStat = new Stats(), tcLoadStat = new Stats();
      tcStoreStats.add(tcStoreStat);
      tcLoadStats.add(tcLoadStat);

      storeAndLoad(scStoreStat, scLoadStat, iterations, sleepycatSerializerMo, sleepycatSerializerPersistor);
      storeAndLoad(tcStoreStat, tcLoadStat, iterations, customSerializerMo, customSerializerPersistor);
    }
    System.err.println("===============================================================");
    System.err.println("Paranoid: " + paranoid + ", field sets: " + fieldSetCount + ", cycles: " + cycles
                       + ", iterations per cycle: " + iterations);
    System.err.println("TC store       : " + summary(tcStoreStats) + "; " + tcStoreStats);
    System.err.println("Sleepycat store: " + summary(sleepycatStoreStats) + "; " + sleepycatStoreStats);
    System.err.println("TC load        : " + summary(tcLoadStats) + "; " + tcLoadStats);
    System.err.println("Sleepycat load : " + summary(sleepycatLoadStats) + "; " + sleepycatLoadStats);

  }

  private void storeAndLoad(Stats storeStat, Stats loadStat, int iterations, ManagedObjectImpl mo,
                            SleepycatPersistor persistor) {
    ManagedObjectPersistor mop = persistor.getManagedObjectPersistor();
    ObjectID objectID = mo.getID();
    long now = System.currentTimeMillis();
    PersistenceTransaction ptx = persistor.getPersistenceTransactionProvider().newTransaction();
    for (int i = 0; i < iterations; i++) {
      mo.setIsDirty(true);
      mop.saveObject(ptx, mo);
    }
    ptx.commit();
    storeStat.time += System.currentTimeMillis() - now;

    ManagedObject test = null;
    now = System.currentTimeMillis();
    for (int i = 0; i < iterations; i++) {
      test = mop.loadObjectByID(objectID);
    }
    loadStat.time += System.currentTimeMillis() - now;
    assertTrue(mo.isEqual(test));
  }

  public void testSerialization() throws Exception {
    ManagedObjectImpl customSerializerMo = newManagedObject();
    ManagedObjectImpl sleepycatSerializerMo = newManagedObject();

    EntryBinding binding = new SerialBinding(classCatalog, sleepycatSerializerMo.getClass());

    int iterations = 100;
    int cycles = 5;

    DatabaseEntry entry = new DatabaseEntry();
    List sleepycatSstats = new ArrayList(cycles);
    List sleepycatDsstats = new ArrayList(cycles);
    List tcSstats = new ArrayList(cycles);
    List tcDsstats = new ArrayList(cycles);

    for (int i = 0; i < cycles; i++) {
      Stats ssstat = new Stats(), sdsstat = new Stats();
      sleepycatSstats.add(ssstat);
      sleepycatDsstats.add(sdsstat);

      Stats tcsstat = new Stats(), tcdsstat = new Stats();
      tcSstats.add(tcsstat);
      tcDsstats.add(tcdsstat);

      sleepycatSerialize(ssstat, sdsstat, iterations, sleepycatSerializerMo, binding, entry);
      tcSerialize(tcsstat, tcdsstat, iterations, customSerializerMo);
    }
    System.err.println("===============================================================");
    System.err.println("Paranoid: " + paranoid + ", field sets: " + fieldSetCount + ", cycles: " + cycles
                       + ", serializations per cycle: " + iterations);
    System.err.println("TC serialization stats         : " + summary(tcSstats) + "; " + tcSstats);
    System.err.println("Sleepycat serialization stats  : " + summary(sleepycatSstats) + "; " + sleepycatSstats);

    System.err.println("TC deserialization stats       : " + summary(tcDsstats) + "; " + tcDsstats);
    System.err.println("Sleepycat deserialization stats: " + summary(sleepycatDsstats) + "; " + sleepycatDsstats);
  }

  private ManagedObjectImpl newManagedObject() {
    ManagedObjectImpl mo = new ManagedObjectImpl(new ObjectID(1));
    TestDNA dna = newDNA();
    mo.apply(dna, new TransactionID(1), new BackReferences(), imo, false);
    return mo;
  }

  private void sleepycatSerialize(Stats serializeStats, Stats deserializeStats, int iterations, ManagedObjectImpl mo,
                                  EntryBinding binding, DatabaseEntry entry) {
    long now = System.currentTimeMillis();
    for (int i = 0; i < iterations; i++) {
      binding.objectToEntry(mo, entry);
    }
    serializeStats.time += System.currentTimeMillis() - now;
    serializeStats.size += entry.getData().length;

    ManagedObject test = null;
    now = System.currentTimeMillis();
    for (int i = 0; i < iterations; i++) {
      test = (ManagedObject) binding.entryToObject(entry);
    }
    deserializeStats.time += System.currentTimeMillis() - now;
    assertTrue(mo.isEqual(test));
  }

  private void tcSerialize(Stats serializeStats, Stats deserializeStats, int iterations, ManagedObjectImpl mo)
      throws IOException, ClassNotFoundException {
    long now = System.currentTimeMillis();
    SerializationAdapter serializer = customSerializerPersistor.getSerializationAdapter();
    DatabaseEntry entry = new DatabaseEntry();
    for (int i = 0; i < iterations; i++) {
      serializer.serializeManagedObject(entry, mo);
    }
    serializeStats.time += System.currentTimeMillis() - now;
    serializeStats.size += entry.getData().length;

    ManagedObject test = null;
    now = System.currentTimeMillis();
    for (int i = 0; i < iterations; i++) {
      test = serializer.deserializeManagedObject(entry);
    }
    deserializeStats.time += System.currentTimeMillis() - now;
    assertTrue(mo.isEqual(test));
  }

  private TestDNA newDNA() {
    TestDNACursor cursor = new TestDNACursor();
    for (int i = 0; i < fieldSetCount; i++) {
      cursor.addPhysicalAction("refField" + i, new ObjectID(1));
      cursor.addPhysicalAction("booleanField" + i, new Boolean(true));
      cursor.addPhysicalAction("byteField" + i, new Byte((byte) 1));
      cursor.addPhysicalAction("characterField" + i, new Character('c'));
      cursor.addPhysicalAction("doubleField" + i, new Double(100.001d));
      cursor.addPhysicalAction("floatField" + i, new Float(100.001f));
      cursor.addPhysicalAction("intField" + i, new Integer(100));
      cursor.addPhysicalAction("longField" + i, new Long(100));
      cursor.addPhysicalAction("shortField" + i, new Short((short) 1));
      cursor.addPhysicalAction("stringField" + i, "This is a nice string to add" + i);
    }
    TestDNA dna = new TestDNA(cursor);
    return dna;
  }

  private DBEnvironment newEnvironment() throws Exception {
    File envHome;
    int counter = 0;
    do {
      envHome = new File(getTempDirectory(), "database-environment-home" + (++counter));
    } while (envHome.exists());
    envHome.mkdir();
    assertTrue(envHome.exists());
    assertTrue(envHome.isDirectory());
    DBEnvironment rv = new DBEnvironment(paranoid, envHome);
    environments.add(rv);
    return rv;
  }

  private Summary summary(Collection stats) {
    Summary avg = new Summary();
    Stats stat = null;
    for (Iterator i = stats.iterator(); i.hasNext();) {
      stat = (Stats) i.next();
      avg.sum += stat.time;
    }
    avg.average = avg.sum / stats.size();
    avg.size = stat.size;
    return avg;
  }

  private static final class Summary {
    public long   sum;
    public double average;
    public long   size;

    public String toString() {
      return "avg: " + average + ", size: " + size;
    }
  }

  private static final class Stats {
    public long time;
    public long size;

    public String toString() {
      return "time: " + time;
    }
  }
}
