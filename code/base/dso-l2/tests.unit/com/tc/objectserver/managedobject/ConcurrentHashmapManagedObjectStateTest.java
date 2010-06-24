/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.io.serializer.TCObjectInputStream;
import com.tc.io.serializer.TCObjectOutputStream;
import com.tc.object.ObjectID;
import com.tc.object.TestDNACursor;
import com.tc.objectserver.core.api.ManagedObjectState;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class ConcurrentHashmapManagedObjectStateTest extends AbstractTestManagedObjectState {

  public void testSerialization() throws Exception {

    final String className = ConcurrentHashMap.class.getName();

    final TestDNACursor cursor = new TestDNACursor();

    cursor.addPhysicalAction(ConcurrentHashMapManagedObjectState.SEGMENT_MASK_FIELD_NAME, new Integer(10), false);
    cursor.addPhysicalAction(ConcurrentHashMapManagedObjectState.SEGMENT_SHIFT_FIELD_NAME, new Integer(20), false);
    final int segment_size = 512;
    final Object[] segments = new Object[segment_size];
    for (int i = 0; i < segment_size; i++) {
      segments[i] = new ObjectID(2000 + i);
    }
    cursor.addArrayAction(segments);

    final ManagedObjectState state = createManagedObjectState(className, cursor);
    state.apply(new ObjectID(1), cursor, new ApplyTransactionInfo());

    final ManagedObjectStateSerializer serializer = new ManagedObjectStateSerializer();

    final ByteArrayOutputStream baout = new ByteArrayOutputStream();
    final TCObjectOutputStream out = new TCObjectOutputStream(baout);

    serializer.serializeTo(state, out);

    final byte seralized[] = baout.toByteArray();
    System.err.println("Serialized size = " + seralized.length);

    final ByteArrayInputStream bi = new ByteArrayInputStream(seralized);
    final TCObjectInputStream in = new TCObjectInputStream(bi);

    final ConcurrentHashMapManagedObjectState state2 = (ConcurrentHashMapManagedObjectState) serializer
        .deserializeFrom(in);
    state2.setMap(new HashMap());

    assertEquals(state, state2);
  }

}
