/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.io.serializer.TCObjectInputStream;
import com.tc.io.serializer.TCObjectOutputStream;
import com.tc.object.ObjectID;
import com.tc.objectserver.core.api.ManagedObjectState;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class ConcurrentHashmapManagedObjectStateTest extends AbstractTestManagedObjectState {

  public void testSerialization() throws Exception {

    String className = ConcurrentHashMap.class.getName();
    String SEGMENT_MASK_FIELD_NAME = "segmentMask";
    String SEGMENT_SHIFT_FIELD_NAME = "segmentShift";

    TestDNACursor cursor = new TestDNACursor();

    cursor.addPhysicalAction(SEGMENT_MASK_FIELD_NAME, new Integer(10), false);
    cursor.addPhysicalAction(SEGMENT_SHIFT_FIELD_NAME, new Integer(20), false);
    int segment_size = 512;
    Object[] segments = new Object[segment_size];
    for (int i = 0; i < segment_size; i++) {
      segments[i] = new ObjectID(2000 + i);
    }
    cursor.addArrayAction(segments);

    ManagedObjectState state = createManagedObjectState(className, cursor);
    state.apply(new ObjectID(1), cursor, new BackReferences());

    ManagedObjectStateSerializer serializer = new ManagedObjectStateSerializer();

    ByteArrayOutputStream baout = new ByteArrayOutputStream();
    TCObjectOutputStream out = new TCObjectOutputStream(baout);

    serializer.serializeTo(state, out);

    byte seralized[] = baout.toByteArray();
    System.err.println("Serialized size = " + seralized.length);

    ByteArrayInputStream bi = new ByteArrayInputStream(seralized);
    TCObjectInputStream in = new TCObjectInputStream(bi);

    ConcurrentHashMapManagedObjectState state2 = (ConcurrentHashMapManagedObjectState) serializer.deserializeFrom(in);
    state2.setMap(new HashMap());

    assertEquals(state, state2);
  }

}
