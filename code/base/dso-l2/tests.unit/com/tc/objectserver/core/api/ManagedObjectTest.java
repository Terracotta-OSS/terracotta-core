/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.core.api;

import com.tc.object.ObjectID;
import com.tc.object.TestDNACursor;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.ObjectInstanceMonitor;
import com.tc.objectserver.impl.ObjectInstanceMonitorImpl;
import com.tc.objectserver.managedobject.ApplyTransactionInfo;
import com.tc.objectserver.managedobject.ManagedObjectImpl;
import com.tc.objectserver.managedobject.ManagedObjectStateFactory;
import com.tc.objectserver.managedobject.NullManagedObjectChangeListenerProvider;
import com.tc.objectserver.persistence.inmemory.InMemoryPersistor;
import com.tc.test.TCTestCase;

import java.util.Map;

public class ManagedObjectTest extends TCTestCase {

  public void testBasics() throws Exception {
    final ObjectInstanceMonitor instanceMonitor = new ObjectInstanceMonitorImpl();
    final ObjectID objectID = new ObjectID(1);
    ManagedObjectStateFactory.disableSingleton(true);
    ManagedObjectStateFactory.createInstance(new NullManagedObjectChangeListenerProvider(), new InMemoryPersistor());

    final ManagedObjectImpl mo = new ManagedObjectImpl(objectID);

    assertTrue(mo.isDirty());
    assertTrue(mo.isNew());

    final TestDNACursor cursor = new TestDNACursor();
    cursor.addPhysicalAction("field1", new ObjectID(1), true);
    cursor.addPhysicalAction("field2", new Boolean(true), true);
    cursor.addPhysicalAction("field3", new Character('c'), true);
    final TestDNA dna = new TestDNA(cursor);

    Map instances = instanceMonitor.getInstanceCounts();
    assertEquals(0, instances.size());
    mo.apply(dna, new TransactionID(1), new ApplyTransactionInfo(), instanceMonitor, false);

    instances = instanceMonitor.getInstanceCounts();
    assertEquals(1, instances.size());
    assertEquals(new Integer(1), instances.get(dna.getTypeName()));

    assertEquals(1, mo.getObjectReferences().size());
    assertEquals(dna.typeName, mo.getClassname());
    assertEquals(dna.loaderDesc, mo.getLoaderDescription());
  }

  public void testApplyDNASameOrLowerVersion() throws Exception {
    final ObjectInstanceMonitor instanceMonitor = new ObjectInstanceMonitorImpl();
    final ObjectID objectID = new ObjectID(1);
    ManagedObjectStateFactory.disableSingleton(true);
    ManagedObjectStateFactory.createInstance(new NullManagedObjectChangeListenerProvider(), new InMemoryPersistor());

    final ManagedObjectImpl mo = new ManagedObjectImpl(objectID);

    final TestDNACursor cursor = new TestDNACursor();
    cursor.addPhysicalAction("field1", new ObjectID(1), true);
    TestDNA dna = new TestDNA(cursor);
    dna.version = 10;

    mo.apply(dna, new TransactionID(1), new ApplyTransactionInfo(), instanceMonitor, false);

    // Now reapply and see if it fails.
    boolean failed = false;
    try {
      mo.apply(dna, new TransactionID(1), new ApplyTransactionInfo(), instanceMonitor, false);
      failed = true;
    } catch (final AssertionError ae) {
      // expected.
    }
    if (failed) { throw new AssertionError("Failed to fail !!!"); }

    // Apply with lower version
    try {
      dna = new TestDNA(cursor);
      dna.version = 5;
      dna.isDelta = true;
      mo.apply(dna, new TransactionID(1), new ApplyTransactionInfo(), instanceMonitor, false);
      failed = true;
    } catch (final AssertionError ae) {
      // expected.
    }
    if (failed) { throw new AssertionError("Failed to fail !!!"); }

    // Apply with higher
    dna = new TestDNA(cursor);
    dna.version = 15;
    dna.isDelta = true;
    mo.apply(dna, new TransactionID(1), new ApplyTransactionInfo(), instanceMonitor, false);

    final long version = mo.getVersion();

    // Apply in passive, ignore as true.
    dna = new TestDNA(cursor);
    dna.version = 5;
    dna.isDelta = true;
    mo.apply(dna, new TransactionID(1), new ApplyTransactionInfo(), instanceMonitor, true);

    assertTrue(version == mo.getVersion());

    dna = new TestDNA(cursor);
    dna.version = 15;
    dna.isDelta = true;
    mo.apply(dna, new TransactionID(1), new ApplyTransactionInfo(), instanceMonitor, true);

    assertTrue(version == mo.getVersion());

    dna = new TestDNA(cursor);
    dna.version = 17;
    dna.isDelta = true;
    mo.apply(dna, new TransactionID(1), new ApplyTransactionInfo(), instanceMonitor, true);

    assertTrue(version < mo.getVersion());
  }

}