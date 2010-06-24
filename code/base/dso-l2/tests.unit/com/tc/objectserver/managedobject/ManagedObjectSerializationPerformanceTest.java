/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.sleepycat.bind.EntryBinding;
import com.sleepycat.bind.serial.SerialBinding;
import com.sleepycat.bind.serial.StoredClassCatalog;
import com.sleepycat.je.DatabaseEntry;
import com.tc.logging.NullTCLogger;
import com.tc.object.ObjectID;
import com.tc.object.TestDNACursor;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.ObjectInstanceMonitor;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.core.api.TestDNA;
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
  private final int             fieldSetCount = 10;
  private SleepycatPersistor    sleepycatSerializerPersistor;
  private SleepycatPersistor    customSerializerPersistor;

  private boolean               paranoid;
  private DBEnvironment         sleepycatSerializerEnvironment;
  private DBEnvironment         customSerializerEnvironment;
  private final Set             environments  = new HashSet();
  private ObjectInstanceMonitor imo;

  @Override
  public void setUp() throws Exception {

    this.paranoid = false;
    this.imo = new ObjectInstanceMonitorImpl();

    // ManagedObjectChangeListenerProvider listenerProvider = new NullManagedObjectChangeListenerProvider();

    // set up sleepycat serializer persistor
    this.sleepycatSerializerEnvironment = newEnvironment();
    final SerializationAdapterFactory saf = new SleepycatSerializationAdapterFactory();
    this.sleepycatSerializerPersistor = new SleepycatPersistor(new NullTCLogger(), this.sleepycatSerializerEnvironment,
                                                               saf);
    this.classCatalog = (StoredClassCatalog) this.sleepycatSerializerEnvironment.getClassCatalogWrapper()
        .getClassCatalog();

    ManagedObjectStateFactory.disableSingleton(true);
    ManagedObjectStateFactory.createInstance(new NullManagedObjectChangeListenerProvider(),
                                             this.sleepycatSerializerPersistor);

    // set up custom serializer persistor
    this.customSerializerEnvironment = newEnvironment();

    final CustomSerializationAdapterFactory customSaf = new CustomSerializationAdapterFactory();
    this.customSerializerPersistor = new SleepycatPersistor(new NullTCLogger(), this.customSerializerEnvironment,
                                                            customSaf);
  }

  @Override
  public void tearDown() throws Exception {
    for (final Iterator i = this.environments.iterator(); i.hasNext();) {
      ((DBEnvironment) i.next()).close();
    }
  }

  public void testStoreAndLoad() throws Exception {
    final ManagedObjectImpl customSerializerMo = newManagedObject();
    final ManagedObjectImpl sleepycatSerializerMo = newManagedObject();

    final int iterations = 100;
    final int cycles = 5;

    final List sleepycatStoreStats = new ArrayList(cycles);
    final List sleepycatLoadStats = new ArrayList(cycles);
    final List tcStoreStats = new ArrayList(cycles);
    final List tcLoadStats = new ArrayList(cycles);
    for (int i = 0; i < cycles; i++) {
      final Stats scStoreStat = new Stats(), scLoadStat = new Stats();
      sleepycatStoreStats.add(scStoreStat);
      sleepycatLoadStats.add(scLoadStat);

      final Stats tcStoreStat = new Stats(), tcLoadStat = new Stats();
      tcStoreStats.add(tcStoreStat);
      tcLoadStats.add(tcLoadStat);

      storeAndLoad(scStoreStat, scLoadStat, iterations, sleepycatSerializerMo, this.sleepycatSerializerPersistor);
      storeAndLoad(tcStoreStat, tcLoadStat, iterations, customSerializerMo, this.customSerializerPersistor);
    }
    System.err.println("===============================================================");
    System.err.println("Paranoid: " + this.paranoid + ", field sets: " + this.fieldSetCount + ", cycles: " + cycles
                       + ", iterations per cycle: " + iterations);
    System.err.println("TC store       : " + summary(tcStoreStats) + "; " + tcStoreStats);
    System.err.println("Sleepycat store: " + summary(sleepycatStoreStats) + "; " + sleepycatStoreStats);
    System.err.println("TC load        : " + summary(tcLoadStats) + "; " + tcLoadStats);
    System.err.println("Sleepycat load : " + summary(sleepycatLoadStats) + "; " + sleepycatLoadStats);

  }

  private void storeAndLoad(final Stats storeStat, final Stats loadStat, final int iterations,
                            final ManagedObjectImpl mo, final SleepycatPersistor persistor) {
    final ManagedObjectPersistor mop = persistor.getManagedObjectPersistor();
    final ObjectID objectID = mo.getID();
    long now = System.currentTimeMillis();
    final PersistenceTransaction ptx = persistor.getPersistenceTransactionProvider().newTransaction();
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
    final ManagedObjectImpl customSerializerMo = newManagedObject();
    final ManagedObjectImpl sleepycatSerializerMo = newManagedObject();

    final EntryBinding binding = new SerialBinding(this.classCatalog, sleepycatSerializerMo.getClass());

    final int iterations = 100;
    final int cycles = 5;

    final DatabaseEntry entry = new DatabaseEntry();
    final List sleepycatSstats = new ArrayList(cycles);
    final List sleepycatDsstats = new ArrayList(cycles);
    final List tcSstats = new ArrayList(cycles);
    final List tcDsstats = new ArrayList(cycles);

    for (int i = 0; i < cycles; i++) {
      final Stats ssstat = new Stats(), sdsstat = new Stats();
      sleepycatSstats.add(ssstat);
      sleepycatDsstats.add(sdsstat);

      final Stats tcsstat = new Stats(), tcdsstat = new Stats();
      tcSstats.add(tcsstat);
      tcDsstats.add(tcdsstat);

      sleepycatSerialize(ssstat, sdsstat, iterations, sleepycatSerializerMo, binding, entry);
      tcSerialize(tcsstat, tcdsstat, iterations, customSerializerMo);
    }
    System.err.println("===============================================================");
    System.err.println("Paranoid: " + this.paranoid + ", field sets: " + this.fieldSetCount + ", cycles: " + cycles
                       + ", serializations per cycle: " + iterations);
    System.err.println("TC serialization stats         : " + summary(tcSstats) + "; " + tcSstats);
    System.err.println("Sleepycat serialization stats  : " + summary(sleepycatSstats) + "; " + sleepycatSstats);

    System.err.println("TC deserialization stats       : " + summary(tcDsstats) + "; " + tcDsstats);
    System.err.println("Sleepycat deserialization stats: " + summary(sleepycatDsstats) + "; " + sleepycatDsstats);
  }

  private ManagedObjectImpl newManagedObject() {
    final ManagedObjectImpl mo = new ManagedObjectImpl(new ObjectID(1));
    final TestDNA dna = newDNA();
    mo.apply(dna, new TransactionID(1), new ApplyTransactionInfo(), this.imo, false);
    return mo;
  }

  private void sleepycatSerialize(final Stats serializeStats, final Stats deserializeStats, final int iterations,
                                  final ManagedObjectImpl mo, final EntryBinding binding, final DatabaseEntry entry) {
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

  private void tcSerialize(final Stats serializeStats, final Stats deserializeStats, final int iterations,
                           final ManagedObjectImpl mo) throws IOException, ClassNotFoundException {
    long now = System.currentTimeMillis();
    final SerializationAdapter serializer = this.customSerializerPersistor.getSerializationAdapter();
    final DatabaseEntry entry = new DatabaseEntry();
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
    final TestDNACursor cursor = new TestDNACursor();
    for (int i = 0; i < this.fieldSetCount; i++) {
      cursor.addPhysicalAction("refField" + i, new ObjectID(1), true);
      cursor.addPhysicalAction("booleanField" + i, new Boolean(true), true);
      cursor.addPhysicalAction("byteField" + i, new Byte((byte) 1), true);
      cursor.addPhysicalAction("characterField" + i, new Character('c'), true);
      cursor.addPhysicalAction("doubleField" + i, new Double(100.001d), true);
      cursor.addPhysicalAction("floatField" + i, new Float(100.001f), true);
      cursor.addPhysicalAction("intField" + i, new Integer(100), true);
      cursor.addPhysicalAction("longField" + i, new Long(100), true);
      cursor.addPhysicalAction("shortField" + i, new Short((short) 1), true);
      cursor.addPhysicalAction("stringField" + i, "This is a nice string to add" + i, true);
    }
    final TestDNA dna = new TestDNA(cursor);
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
    final DBEnvironment rv = new DBEnvironment(this.paranoid, envHome);
    this.environments.add(rv);
    return rv;
  }

  private Summary summary(final Collection stats) {
    final Summary avg = new Summary();
    Stats stat = null;
    for (final Iterator i = stats.iterator(); i.hasNext();) {
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

    @Override
    public String toString() {
      return "avg: " + this.average + ", size: " + this.size;
    }
  }

  private static final class Stats {
    public long time;
    public long size;

    @Override
    public String toString() {
      return "time: " + this.time;
    }
  }
}
