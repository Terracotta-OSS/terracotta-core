/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.core.api;

import com.tc.object.ObjectID;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.ObjectInstanceMonitor;
import com.tc.objectserver.impl.ObjectInstanceMonitorImpl;
import com.tc.objectserver.managedobject.BackReferences;
import com.tc.objectserver.managedobject.ManagedObjectImpl;
import com.tc.objectserver.managedobject.ManagedObjectStateFactory;
import com.tc.objectserver.managedobject.NullManagedObjectChangeListenerProvider;
import com.tc.objectserver.persistence.impl.InMemoryPersistor;
import com.tc.test.TCTestCase;

import java.util.Map;

public class ManagedObjectTest extends TCTestCase {

  public void testBasics() throws Exception {
    ObjectInstanceMonitor instanceMonitor = new ObjectInstanceMonitorImpl();
    ObjectID objectID = new ObjectID(1);
    ManagedObjectStateFactory.disableSingleton(true);
    ManagedObjectStateFactory.createInstance(new NullManagedObjectChangeListenerProvider(), 
                                             new InMemoryPersistor());

    ManagedObjectImpl mo = new ManagedObjectImpl(objectID);

    assertTrue(mo.isDirty());
    assertTrue(mo.isNew());

    TestDNACursor cursor = new TestDNACursor();
    cursor.addPhysicalAction("field1", new ObjectID(1));
    cursor.addPhysicalAction("field2", new Boolean(true));
    cursor.addPhysicalAction("field3", new Character('c'));
    TestDNA dna = new TestDNA(cursor);
    
    Map instances = instanceMonitor.getInstanceCounts();
    assertEquals(0, instances.size());
    mo.apply(dna, new TransactionID(1), new BackReferences(), instanceMonitor);
    assertFalse(mo.isNew());
    
    instances = instanceMonitor.getInstanceCounts();
    assertEquals(1, instances.size());
    assertEquals(new Integer(1), instances.get(dna.getTypeName()));

    assertEquals(1, mo.getObjectReferences().size());
    assertEquals(dna.typeName, mo.getClassname());
    assertEquals(dna.loaderDesc, mo.getLoaderDescription());
  }

}