/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.object.ObjectID;
import com.tc.object.SerializationUtil;
import com.tc.object.TestDNACursor;
import com.tc.object.TestDNAWriter;
import com.tc.object.dna.api.DNA.DNAType;
import com.tc.objectserver.core.api.ManagedObjectState;
import com.tc.util.Assert;

public class QueueManagedObjectStateTest extends AbstractTestManagedObjectState {
  String     className            = "java.util.concurrent.LinkedBlockingQueue";
  String     TAKE_LOCK_FIELD_NAME = "takeLock";
  String     PUT_LOCK_FIELD_NAME  = "putLock";
  String     CAPACITY_FIELD_NAME  = "capacity";
  final byte type                 = ManagedObjectState.QUEUE_TYPE;

  public void basicTestQueue(final TestDNACursor cursor, final int objCount, final int actionCount) throws Exception {
    final ManagedObjectState state = createManagedObjectState(this.className, cursor);
    state.apply(this.objectID, cursor, new ApplyTransactionInfo());

    // API verification
    basicAPI(this.className, this.type, cursor, objCount, state);

    // dehydrate
    basicDehydrate(cursor, actionCount, state);

    // writeTo, readFrom and equal
    basicReadWriteEqual(this.type, state);
  }

  // override due to difference on dehydrate
  @Override
  protected void basicDehydrate(final TestDNACursor cursor, final int actionCount, final ManagedObjectState state) {
    final TestDNAWriter dnaWriter = new TestDNAWriter();
    state.dehydrate(this.objectID, dnaWriter, DNAType.L1_FAULT);
    Assert.assertEquals(actionCount, dnaWriter.getActionCount());
  }

  public void testObjectQueue1() throws Exception {
    final TestDNACursor cursor = new TestDNACursor();

    cursor.addPhysicalAction(this.TAKE_LOCK_FIELD_NAME, new ObjectID(2001), true);
    cursor.addPhysicalAction(this.PUT_LOCK_FIELD_NAME, new ObjectID(2002), true);
    cursor.addPhysicalAction(this.CAPACITY_FIELD_NAME, new Integer(100), false);

    cursor.addLogicalAction(SerializationUtil.PUT, new Object[] { new ObjectID(2003) });
    cursor.addLogicalAction(SerializationUtil.PUT, new Object[] { new ObjectID(2004) });

    basicTestQueue(cursor, 4, 5);
  }

  public void testObjectQueue2() throws Exception {
    final TestDNACursor cursor = new TestDNACursor();

    cursor.addLogicalAction(SerializationUtil.PUT, new Object[] { new ObjectID(2003) });
    cursor.addLogicalAction(SerializationUtil.PUT, new Object[] { new ObjectID(2004) });
    cursor.addLogicalAction(SerializationUtil.PUT, new Object[] { new ObjectID(2005) });
    cursor.addLogicalAction(SerializationUtil.PUT, new Object[] { new ObjectID(2006) });
    cursor.addLogicalAction(SerializationUtil.PUT, new Object[] { new ObjectID(2007) });
    cursor.addLogicalAction(SerializationUtil.PUT, new Object[] { new ObjectID(2008) });
    cursor.addLogicalAction(SerializationUtil.PUT, new Object[] { new ObjectID(2009) });
    cursor.addLogicalAction(SerializationUtil.REMOVE_AT, new Object[] { new Integer(0) });

    basicTestQueue(cursor, 6, 9);
  }

}
