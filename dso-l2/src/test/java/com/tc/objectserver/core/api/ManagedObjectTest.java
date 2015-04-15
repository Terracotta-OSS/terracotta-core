/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.objectserver.core.api;

import org.junit.experimental.categories.Category;
import org.terracotta.test.categories.CheckShorts;

import com.tc.object.LogicalOperation;
import com.tc.object.ObjectID;
import com.tc.object.TestDNACursor;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.ObjectInstanceMonitor;
import com.tc.objectserver.impl.ObjectInstanceMonitorImpl;
import com.tc.objectserver.managedobject.ApplyTransactionInfo;
import com.tc.objectserver.managedobject.ManagedObjectImpl;
import com.tc.objectserver.managedobject.ManagedObjectStateFactory;
import com.tc.objectserver.managedobject.NullManagedObjectChangeListenerProvider;
import com.tc.objectserver.persistence.HeapStorageManagerFactory;
import com.tc.objectserver.persistence.Persistor;
import com.tc.test.TCTestCase;

import java.util.Map;

@Category(CheckShorts.class)
public class ManagedObjectTest extends TCTestCase {

  static {
    ManagedObjectStateFactory.disableSingleton(true);
  }

  private Persistor persistor;

  @Override
  public void setUp() {
    persistor = new Persistor(HeapStorageManagerFactory.INSTANCE);
    persistor.start();
    ManagedObjectStateFactory.createInstance(new NullManagedObjectChangeListenerProvider(), persistor);
  }

  public void testBasics() throws Exception {
    final ObjectInstanceMonitor instanceMonitor = new ObjectInstanceMonitorImpl();
    final ObjectID objectID = new ObjectID(1);
    final ManagedObjectImpl mo = new ManagedObjectImpl(objectID, persistor.getManagedObjectPersistor());

    assertTrue(mo.isDirty());
    assertTrue(mo.isNew());

    final TestDNACursor cursor = new TestDNACursor();
    cursor.addLogicalAction(LogicalOperation.PUT, new Object[] { Integer.valueOf(10), "King Kong" });
    cursor.addLogicalAction(LogicalOperation.PUT, new Object[] { Integer.valueOf(20), "Mad Max" });
    cursor.addLogicalAction(LogicalOperation.PUT, new Object[] { Integer.valueOf(25), "Mummy Returns" });
    cursor.addLogicalAction(LogicalOperation.PUT, new Object[] { Integer.valueOf(100), new ObjectID(10000) });
    final TestDNA dna = new TestDNA(cursor);

    Map instances = instanceMonitor.getInstanceCounts();
    assertEquals(0, instances.size());
    mo.apply(dna, new TransactionID(1), new ApplyTransactionInfo(), instanceMonitor, false);

    instances = instanceMonitor.getInstanceCounts();
    assertEquals(1, instances.size());
    assertEquals(Integer.valueOf(1), instances.get(dna.getTypeName()));

    assertEquals(1, mo.getObjectReferences().size());
    assertEquals(dna.typeName, mo.getClassname());
  }

  public void testIsNewToDB() throws Exception {
    final ObjectID objectID = new ObjectID(1);

    final ManagedObjectImpl mo = new ManagedObjectImpl(objectID, persistor.getManagedObjectPersistor());

    assertTrue(mo.isDirty());
    assertTrue(mo.isNew());

    mo.setIsDirty(true);
    assertTrue(mo.isDirty());

    mo.setIsDirty(false);
    assertFalse(mo.isDirty());

    mo.setIsDirty(true);
    assertTrue(mo.isDirty());

    mo.setIsDirty(false);
    assertFalse(mo.isDirty());

  }

  public void testApplyDNASameOrLowerVersion() throws Exception {
    final ObjectInstanceMonitor instanceMonitor = new ObjectInstanceMonitorImpl();
    final ObjectID objectID = new ObjectID(1);

    final ManagedObjectImpl mo = new ManagedObjectImpl(objectID, persistor.getManagedObjectPersistor());

    final TestDNACursor cursor = new TestDNACursor();
    cursor.addLogicalAction(LogicalOperation.PUT, new Object[] { Integer.valueOf(10), "King Kong" });
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
