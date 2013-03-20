/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.google.common.eventbus.Subscribe;
import com.tc.object.ObjectID;
import com.tc.object.SerializationUtil;
import com.tc.object.TestDNACursor;
import com.tc.object.TestDNAWriter;
import com.tc.object.dna.api.DNA.DNAType;
import com.tc.objectserver.core.api.ManagedObjectState;
import com.tc.util.Assert;
import com.tc.util.Events;

public class ConcurrentDistributedServerMapManagedObjectStateTest extends AbstractTestManagedObjectState {

  public void testDehydration() throws Exception {

    final TestDNACursor cursor = createDehydrationDNACursor();

    final ManagedObjectState state = createManagedObjectState(ManagedObjectStateStaticConfig.ToolkitTypeNames.SERVER_MAP_TYPE,
        cursor, new ObjectID(1));
    state.apply(new ObjectID(1), cursor, new ApplyTransactionInfo());

    TestDNAWriter dnaWriter = new TestDNAWriter();
    state.dehydrate(new ObjectID(1), dnaWriter, DNAType.L2_SYNC); // fully Dehydrate
    Assert.assertEquals(cursor.getActionCount(), dnaWriter.getActionCount());

    final TestDNACursor cursor2 = dnaWriter.getDNACursor();
    final ManagedObjectState state2 = createManagedObjectState(ManagedObjectStateStaticConfig.ToolkitTypeNames.SERVER_MAP_TYPE,
        cursor2, new ObjectID(2));
    state2.apply(new ObjectID(2), cursor2, new ApplyTransactionInfo());
    assertEquals(state, state2);

    dnaWriter = new TestDNAWriter();
    state.dehydrate(new ObjectID(1), dnaWriter, DNAType.L1_FAULT); // Only dehydrate the fields
    Assert.assertEquals(11, dnaWriter.getActionCount()); // only the physical fields are dehydrated

    final TestDNACursor cursor3 = dnaWriter.getDNACursor();
    final ConcurrentDistributedServerMapManagedObjectState state3 = (ConcurrentDistributedServerMapManagedObjectState)createManagedObjectState(ManagedObjectStateStaticConfig.ToolkitTypeNames.SERVER_MAP_TYPE,
        cursor3, new ObjectID(3));
    state3.apply(new ObjectID(3), cursor3, new ApplyTransactionInfo());
    assertTrue(state3.references.size() == 0);

  }

  public void testShouldSendOperationCountChangeEventOnEachPut() throws Exception {
    final TestDNACursor cursor = createOperationRateDNACursor();
    final ConcurrentDistributedServerMapManagedObjectState state = (ConcurrentDistributedServerMapManagedObjectState)
        createManagedObjectState(ManagedObjectStateStaticConfig.ToolkitTypeNames.SERVER_MAP_TYPE, cursor, new ObjectID(3));
    final OperationCountChangeEventListener listener = new OperationCountChangeEventListener();
    state.getOperationEventBus().register(listener);
    state.apply(new ObjectID(3), cursor, new ApplyTransactionInfo());
    assertEquals(200, listener.count);
  }

  public static final class OperationCountChangeEventListener {
    private int count;

    @Subscribe
    public void recordOperationCountIncrementEvent(Events.OperationCountIncrementEvent event) {
      this.count++;
    }
  }

  private TestDNACursor createDehydrationDNACursor() {

    final TestDNACursor cursor = new TestDNACursor();
    cursor.addPhysicalAction(ConcurrentDistributedServerMapManagedObjectState.CACHE_NAME_FIELDNAME, "bob", false);
    cursor.addPhysicalAction(ConcurrentDistributedServerMapManagedObjectState.INVALIDATE_ON_CHANGE_FIELDNAME,
        false, false);
    cursor.addPhysicalAction(ConcurrentDistributedServerMapManagedObjectState.LOCK_TYPE_FIELDNAME, 1, false);
    cursor.addPhysicalAction(ConcurrentDistributedServerMapManagedObjectState.LOCAL_CACHE_ENABLED_FIELDNAME,
        true, false);
    cursor.addPhysicalAction(ConcurrentDistributedServerMapManagedObjectState.MAX_TTI_SECONDS_FIELDNAME,
        0, false);
    cursor.addPhysicalAction(ConcurrentDistributedServerMapManagedObjectState.MAX_TTL_SECONDS_FIELDNAME,
        0, false);
    cursor.addPhysicalAction(ConcurrentDistributedServerMapManagedObjectState.MAX_COUNT_IN_CLUSTER_FIELDNAME,
        0, false);
    cursor.addPhysicalAction(ConcurrentDistributedServerMapManagedObjectState.COMPRESSION_ENABLED_FIELDNAME,
        false, false);
    cursor.addPhysicalAction(ConcurrentDistributedServerMapManagedObjectState.COPY_ON_READ_ENABLED_FIELDNAME,
        false, false);
    cursor.addPhysicalAction(ConcurrentDistributedServerMapManagedObjectState.EVICTION_ENABLED_FIELDNAME,
        false, false);
    cursor.addPhysicalAction(ConcurrentDistributedServerMapManagedObjectState.BROADCAST_EVICTIONS_FIELDNAME, false, false);

    for (int i = 0; i < 500; i++) {
      cursor.addLogicalAction(SerializationUtil.PUT, new Object[] { "key-" + i, new ObjectID(1000 + i) });
    }

    return cursor;
  }

  private TestDNACursor createOperationRateDNACursor() {

    final TestDNACursor cursor = new TestDNACursor();
    cursor.addPhysicalAction(ConcurrentDistributedServerMapManagedObjectState.CACHE_NAME_FIELDNAME, "bob", false);
    cursor.addPhysicalAction(ConcurrentDistributedServerMapManagedObjectState.INVALIDATE_ON_CHANGE_FIELDNAME,
        false, false);
    cursor.addPhysicalAction(ConcurrentDistributedServerMapManagedObjectState.LOCK_TYPE_FIELDNAME, 1, false);


    for (int i = 0; i < 50; i++) {
      cursor.addLogicalAction(SerializationUtil.PUT, new Object[] { "key-" + (1000 + i), new ObjectID(1000 + i) });
    }
    for (int i = 0; i < 50; i++) {
      cursor.addLogicalAction(SerializationUtil.PUT_IF_ABSENT, new Object[] { "key-" + (2000 + i), new ObjectID(2000 + i) });
    }
    for (int i = 0; i < 50; i++) {
      cursor.addLogicalAction(SerializationUtil.REMOVE, new Object[] { "key-" + (1000 + i) });
    }
    for (int i = 0; i < 50; i++) {
      cursor.addLogicalAction(SerializationUtil.REPLACE, new Object[] { "key-" + (2000 + i), new ObjectID(5000 + i) });
    }

    return cursor;
  }

}
