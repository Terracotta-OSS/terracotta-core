/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.io.serializer.TCObjectInputStream;
import com.tc.io.serializer.TCObjectOutputStream;
import com.tc.object.ObjectID;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.ObjectInstanceMonitor;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.core.api.TestDNA;
import com.tc.objectserver.core.api.TestDNACursor;
import com.tc.objectserver.impl.ObjectInstanceMonitorImpl;
import com.tc.objectserver.persistence.impl.InMemoryPersistor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import junit.framework.TestCase;

public class ManagedObjectSerializerTest extends TestCase {

  private ObjectID                     id;
  private ManagedObjectStateSerializer stateSerializer;

  public void test() throws Exception {
    ManagedObjectStateFactory.disableSingleton(true);
    ManagedObjectStateFactory.createInstance(new NullManagedObjectChangeListenerProvider(), new InMemoryPersistor());

    stateSerializer = new ManagedObjectStateSerializer();
    id = new ObjectID(1);

    ManagedObjectSerializer mos = new ManagedObjectSerializer(stateSerializer);
    ManagedObjectImpl mo = new ManagedObjectImpl(id);
    assertTrue(mo.isDirty());
    assertTrue(mo.isNew());
    TestDNA dna = newDNA(1);
    ObjectInstanceMonitor imo = new ObjectInstanceMonitorImpl();
    mo.apply(dna, new TransactionID(1), new BackReferences(), imo, false);
    assertFalse(mo.isNew());

    ByteArrayOutputStream baout = new ByteArrayOutputStream();
    TCObjectOutputStream out = new TCObjectOutputStream(baout);
    mos.serializeTo(mo, out);
    out.flush();
    ManagedObject mo2 = (ManagedObject) mos.deserializeFrom(new TCObjectInputStream(new ByteArrayInputStream(baout
        .toByteArray())));

    assertFalse(mo2.isDirty());
    assertFalse(mo2.isNew());
    mo.setIsDirty(false);
    assertNotSame(mo, mo2);
    assertTrue(mo.isEqual(mo2));
  }

  private TestDNA newDNA(int fieldSetCount) {
    TestDNACursor cursor = new TestDNACursor();
    for (int i = 0; i < fieldSetCount; i++) {
      cursor.addPhysicalAction("refField" + i, new ObjectID(1));
      cursor.addPhysicalAction("booleanField" + i, new Boolean(true));
      cursor.addPhysicalAction("byteField" + i, new Byte((byte) 1));
      cursor.addPhysicalAction("characterField" + i, new Character('c'));
      cursor.addPhysicalAction("doubleField" + i, new Double(100.001d));
      cursor.addPhysicalAction("floatField" + i, new Float(100.001f));
      cursor.addPhysicalAction("integerField" + i, new Integer(100));
      cursor.addPhysicalAction("longField" + i, new Long(100));
      cursor.addPhysicalAction("stringField" + i, "Some nice string field" + i);
      cursor.addPhysicalAction("shortField" + i, new Short((short) 1));
    }
    TestDNA dna = new TestDNA(cursor);
    return dna;
  }
}
