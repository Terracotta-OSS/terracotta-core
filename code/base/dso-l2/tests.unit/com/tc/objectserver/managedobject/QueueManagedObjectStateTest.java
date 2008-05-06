/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.object.ObjectID;
import com.tc.object.SerializationUtil;
import com.tc.objectserver.core.api.ManagedObjectState;
import com.tc.util.Assert;

public class QueueManagedObjectStateTest extends AbstractTestManagedObjectState {
  String className = "java.util.concurrent.LinkedBlockingQueue";
  String TAKE_LOCK_FIELD_NAME = "java.util.concurrent.LinkedBlockingQueue.takeLock";
  String PUT_LOCK_FIELD_NAME = "java.util.concurrent.LinkedBlockingQueue.putLock";
  String CAPACITY_FIELD_NAME = "java.util.concurrent.LinkedBlockingQueue.capacity";
  final byte type = ManagedObjectState.QUEUE_TYPE;
  
  public void basicTestQueue(TestDNACursor cursor, int objCount, int actionCount) throws Exception {
    ManagedObjectState state = createManagedObjectState(className, cursor);
    state.apply(objectID, cursor, new BackReferences());

    // API verification
    basicAPI(className, type, cursor, objCount, state);
    
    // dehydrate
    basicDehydrate(cursor, actionCount, state);
    
    // writeTo, readFrom and equal
    basicReadWriteEqual(type, state);
  }

  // override due to difference on dehydrate
  protected void basicDehydrate(TestDNACursor cursor, int actionCount, ManagedObjectState state) {
    TestDNAWriter dnaWriter = new TestDNAWriter();
    state.dehydrate(objectID, dnaWriter);
    Assert.assertEquals(actionCount, dnaWriter.getActionCount());
  }

  public void testObjectQueue1() throws Exception {
    TestDNACursor cursor = new TestDNACursor();

    cursor.addPhysicalAction(TAKE_LOCK_FIELD_NAME, new ObjectID(2001), true);
    cursor.addPhysicalAction(PUT_LOCK_FIELD_NAME, new ObjectID(2002), true);
    cursor.addPhysicalAction(CAPACITY_FIELD_NAME, new Integer(100), false);

    cursor.addLogicalAction(SerializationUtil.PUT, new Object[] { new ObjectID(2003) });
    cursor.addLogicalAction(SerializationUtil.PUT, new Object[] { new ObjectID(2004) });

    basicTestQueue(cursor, 4, 5);
  }
  
  public void testObjectQueue2() throws Exception {
    TestDNACursor cursor = new TestDNACursor();

    cursor.addLogicalAction(SerializationUtil.PUT, new Object[] { new ObjectID(2003) });
    cursor.addLogicalAction(SerializationUtil.PUT, new Object[] { new ObjectID(2004) });
    cursor.addLogicalAction(SerializationUtil.PUT, new Object[] { new ObjectID(2005) });
    cursor.addLogicalAction(SerializationUtil.PUT, new Object[] { new ObjectID(2006) });
    cursor.addLogicalAction(SerializationUtil.PUT, new Object[] { new ObjectID(2007) });
    cursor.addLogicalAction(SerializationUtil.PUT, new Object[] { new ObjectID(2008) });
    cursor.addLogicalAction(SerializationUtil.PUT, new Object[] { new ObjectID(2009) });
    cursor.addLogicalAction(SerializationUtil.REMOVE_AT, new Object[] { new Integer(0) });
    
    basicTestQueue( cursor, 6, 9);
  }

}
