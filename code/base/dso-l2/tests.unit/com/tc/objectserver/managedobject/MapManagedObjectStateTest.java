/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.object.ObjectID;
import com.tc.object.SerializationUtil;
import com.tc.objectserver.core.api.ManagedObjectState;

public class MapManagedObjectStateTest extends AbstractTestManagedObjectState {

  public void testConcurentHashMap() throws Exception {
    // MNK-472
    if (true) return;
    
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

    basicTestUnit(className, ManagedObjectState.CONCURRENT_HASHMAP_TYPE, cursor, 7, false);
  }

  public void testTreeMap() throws Exception {
    String className = "java.util.TreeMap";
    String COMPARATOR_FIELDNAME = "java.util.TreeMap.comparator";

    TestDNACursor cursor = new TestDNACursor();

    cursor.addPhysicalAction(COMPARATOR_FIELDNAME, new ObjectID(2001), true);
    cursor.addLogicalAction(SerializationUtil.PUT, new Object[] { new ObjectID(2002), new ObjectID(2003) });
    cursor.addLogicalAction(SerializationUtil.PUT, new Object[] { new ObjectID(2004), new ObjectID(2005) });

    basicTestUnit(className, ManagedObjectState.TREE_MAP_TYPE, cursor, 5, false);
  }

  public void testLinkedHashMap() throws Exception {
    String className = "java.util.LinkedHashMap";
    String ACCESS_ORDER_FIELDNAME = "java.util.LinkedHashMap.accessOrder";

    TestDNACursor cursor = new TestDNACursor();

    cursor.addPhysicalAction(ACCESS_ORDER_FIELDNAME, Boolean.FALSE, false);

    cursor.addLogicalAction(SerializationUtil.PUT, new Object[] { new ObjectID(2002), new ObjectID(2003) });
    cursor.addLogicalAction(SerializationUtil.PUT, new Object[] { new ObjectID(2004), new ObjectID(2005) });

    basicTestUnit(className, ManagedObjectState.LINKED_HASHMAP_TYPE, cursor, 4);
  }

  /*
  public void testIdentityHashMap() throws Exception {
    String className = "java.util.IdentityHashMap";

    TestDNACursor cursor = new TestDNACursor();

    cursor.addLogicalAction(SerializationUtil.PUT, new Object[] { new ObjectID(2012), new ObjectID(2003) });
    cursor.addLogicalAction(SerializationUtil.PUT, new Object[] { new ObjectID(2004), new ObjectID(2015) });

    basicTestUnit(className, ManagedObjectState.MAP_TYPE, cursor, 4);
    // failed on equal, no implementation for basicWriteTo()
  }
  */

}
