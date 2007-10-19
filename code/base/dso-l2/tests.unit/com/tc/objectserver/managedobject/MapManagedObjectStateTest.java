/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.io.serializer.TCObjectInputStream;
import com.tc.io.serializer.TCObjectOutputStream;
import com.tc.object.ObjectID;
import com.tc.object.SerializationUtil;
import com.tc.objectserver.core.api.ManagedObjectState;
import com.tc.util.Assert;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class MapManagedObjectStateTest extends AbstractTestManagedObjectState {

  public void basicTestMap(String className, final byte type, TestDNACursor cursor, int objCount) throws Exception {
    ManagedObjectState state = createManagedObjectState(className);
    state.apply(objectID, cursor, new BackReferences());

    // API verification
    Assert.assertTrue("BackReferences size="+state.getObjectReferences().size(), 
                      state.getObjectReferences().size() == objCount);
    Assert.assertTrue(state.getType() == type);
    Assert.assertTrue("ClassName:"+state.getClassName(), state.getClassName().equals(className));
    
    // dehydrate
    TestDNAWriter dnaWriter = new TestDNAWriter();
    state.dehydrate(objectID, dnaWriter);
    cursor.reset();
    cursor.next();
    while(cursor.next()) {
      Object action = cursor.getAction();
      Assert.assertTrue(dnaWriter.containsAction(action));
    }
    
    // writeTo, readFrom and equal
    ByteArrayOutputStream bout = new ByteArrayOutputStream();
    TCObjectOutputStream out = new TCObjectOutputStream(bout);
    state.writeTo(out);
    ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
    TCObjectInputStream in = new TCObjectInputStream(bin);
    ManagedObjectState state2 = ManagedObjectStateFactory.getInstance().readManagedObjectStateFrom(in, type);
    Assert.assertTrue(state.equals(state2));
  }

  public void testConcurentHashMap() throws Exception {
    String className = "java.util.concurrent.ConcurrentHashMap";
    String SEGMENT_MASK_FIELD_NAME = className + ".segmentMask";
    String SEGMENT_SHIFT_FIELD_NAME = className + ".segmentShift";
    String SEGMENT_FIELD_NAME = className + ".segments";

    TestDNACursor cursor = new TestDNACursor();
    
    cursor.addPhysicalAction(SEGMENT_MASK_FIELD_NAME, new Integer(10), false);
    cursor.addPhysicalAction(SEGMENT_SHIFT_FIELD_NAME, new Integer(20), false);
    cursor.addLiteralAction(new Integer(2));
    cursor.addPhysicalAction(SEGMENT_FIELD_NAME+0, new ObjectID(2001), true);
    cursor.addPhysicalAction(SEGMENT_FIELD_NAME+1, new ObjectID(2002), true);

    cursor.addLogicalAction(SerializationUtil.PUT, new Object[] { new ObjectID(2002), new ObjectID(2003) });
    cursor.addLogicalAction(SerializationUtil.PUT, new Object[] { new ObjectID(2004), new ObjectID(2005) });
    cursor.addLogicalAction(SerializationUtil.PUT, new Object[] { new ObjectID(2009), new ObjectID(2010) });

    basicTestMap(className, ManagedObjectState.CONCURRENT_HASHMAP_TYPE, cursor, 7);
  }
  
  public void testTreeMap() throws Exception {
    String className = "java.util.TreeMap";
    String COMPARATOR_FIELDNAME = "java.util.TreeMap.comparator";

    TestDNACursor cursor = new TestDNACursor();

    cursor.addPhysicalAction(COMPARATOR_FIELDNAME, new ObjectID(2001), true);
    cursor.addLogicalAction(SerializationUtil.PUT, new Object[] { new ObjectID(2002), new ObjectID(2003) });
    cursor.addLogicalAction(SerializationUtil.PUT, new Object[] { new ObjectID(2004), new ObjectID(2005) });
    
    basicTestMap(className, ManagedObjectState.TREE_MAP_TYPE, cursor, 5);
  }

  public void testLinkedHashMap() throws Exception {
    String className = "java.util.LinkedHashMap";
    String ACCESS_ORDER_FIELDNAME = "java.util.LinkedHashMap.accessOrder";

    TestDNACursor cursor = new TestDNACursor();

    cursor.addPhysicalAction(ACCESS_ORDER_FIELDNAME, Boolean.FALSE, false);

    cursor.addLogicalAction(SerializationUtil.PUT, new Object[] { new ObjectID(2002), new ObjectID(2003) });
    cursor.addLogicalAction(SerializationUtil.PUT, new Object[] { new ObjectID(2004), new ObjectID(2005) });
    
    basicTestMap(className, ManagedObjectState.LINKED_HASHMAP_TYPE, cursor, 4);
  }

  /*
  public void testIdentityHashMap() throws Exception {
    String className = "java.util.IdentityHashMap";

    TestDNACursor cursor = new TestDNACursor();

    cursor.addLogicalAction(SerializationUtil.PUT, new Object[] { new ObjectID(2012), new ObjectID(2003) });
    cursor.addLogicalAction(SerializationUtil.PUT, new Object[] { new ObjectID(2004), new ObjectID(2015) });
    
    basicTestMap(className, ManagedObjectState.MAP_TYPE, cursor, 4);
    // failed on equal, no implementation for basicWriteTo()
  }
  */

}
