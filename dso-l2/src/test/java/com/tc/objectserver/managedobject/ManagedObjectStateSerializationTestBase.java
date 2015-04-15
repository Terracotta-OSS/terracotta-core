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
package com.tc.objectserver.managedobject;

import com.tc.object.ObjectID;
import com.tc.object.TestDNAWriter;
import com.tc.object.dna.api.DNA.DNAType;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.NullObjectInstanceMonitor;
import com.tc.objectserver.api.Transaction;
import com.tc.objectserver.api.TransactionProvider;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.core.api.ManagedObjectState;
import com.tc.objectserver.core.api.TestDNA;
import com.tc.objectserver.impl.PersistentManagedObjectStore;
import com.tc.objectserver.persistence.HeapStorageManagerFactory;
import com.tc.objectserver.persistence.ManagedObjectPersistor;
import com.tc.objectserver.persistence.Persistor;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;
import com.tc.util.runtime.ThreadDumpUtil;


public abstract class ManagedObjectStateSerializationTestBase extends TCTestCase {
  private ObjectID                       objectID = new ObjectID(2000);

  private ManagedObjectPersistor managedObjectPersistor;
  private Persistor persistor;
  private TransactionProvider ptp;
  private PersistentManagedObjectStore store;

  @Override
  public void setUp() throws Exception {
    super.setUp();

    this.persistor = new Persistor(HeapStorageManagerFactory.INSTANCE);
    persistor.start();

    this.ptp = persistor.getPersistenceTransactionProvider();
    this.managedObjectPersistor = persistor.getManagedObjectPersistor();
    
    ManagedObjectStateFactory.disableSingleton(true);
    ManagedObjectStateFactory.createInstance(new NullManagedObjectChangeListenerProvider(), persistor);

    store = new PersistentManagedObjectStore(persistor.getManagedObjectPersistor());
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    try {
      this.managedObjectPersistor.close();
      this.persistor.close();
    } catch (Exception e) {
      System.err.println("Closing failed, printing stack trace...");
      System.err.println(ThreadDumpUtil.getThreadDump());
    }
    ManagedObjectStateFactory.disableSingleton(false);
  }

  protected ManagedObjectState applyValidation(final String className, final DNACursor dnaCursor) throws Exception {
    this.objectID = new ObjectID(this.objectID.toLong() + 1);
    final ManagedObject mo = store.createObject(objectID);

    final TestDNA dna = new TestDNA(dnaCursor);
    dna.typeName = className;
    mo.apply(dna, new TransactionID(1), new ApplyTransactionInfo(), new NullObjectInstanceMonitor(), false);

    final Transaction txn = this.ptp.newTransaction();
    this.managedObjectPersistor.saveObject(txn, mo);
    txn.commit();

    final ManagedObjectState state = mo.getManagedObjectState();
    final TestDNAWriter dnaWriter = dehydrate(state);
    validate(dnaCursor, dnaWriter);

    return state;
  }

  protected void serializationValidation(final ManagedObjectState state, final DNACursor dnaCursor, final byte type)
      throws Exception {
    final ManagedObject loaded = this.managedObjectPersistor.loadObjectByID(this.objectID);
    final TestDNAWriter dnaWriter = dehydrate(loaded.getManagedObjectState());
    validate(dnaCursor, dnaWriter);
  }

  private TestDNAWriter dehydrate(final ManagedObjectState state) throws Exception {
    final TestDNAWriter dnaWriter = new TestDNAWriter();
    state.dehydrate(this.objectID, dnaWriter, DNAType.L2_SYNC);
    return dnaWriter;
  }

  private void validate(final DNACursor dnaCursor, final TestDNAWriter writer) throws Exception {
    Assert.assertEquals(dnaCursor.getActionCount(), writer.getActionCount());
    dnaCursor.reset();
    while (dnaCursor.next()) {
      final Object action = dnaCursor.getAction();
      Assert.assertTrue(writer.containsAction(action));
    }
  }
}
