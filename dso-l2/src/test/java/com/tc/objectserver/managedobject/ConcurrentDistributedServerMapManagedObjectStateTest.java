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

public class ConcurrentDistributedServerMapManagedObjectStateTest extends AbstractTestManagedObjectState {

  public static final String CDSM_DSO_CLASSNAME = "com.terracotta.toolkit.collections.ConcurrentDistributedServerMapDso";

  public void testDehydration() throws Exception {

    final TestDNACursor cursor = createDNACursor();

    final ManagedObjectState state = createManagedObjectState(CDSM_DSO_CLASSNAME, cursor);
    state.apply(new ObjectID(1), cursor, new ApplyTransactionInfo());

    TestDNAWriter dnaWriter = new TestDNAWriter();
    state.dehydrate(new ObjectID(1), dnaWriter, DNAType.L2_SYNC); // fully Dehydrate
    Assert.assertEquals(cursor.getActionCount(), dnaWriter.getActionCount());

    final TestDNACursor cursor2 = dnaWriter.getDNACursor();
    final ManagedObjectState state2 = createManagedObjectState(CDSM_DSO_CLASSNAME, cursor2);
    state2.apply(new ObjectID(1), cursor2, new ApplyTransactionInfo());
    assertEquals(state, state2);

    dnaWriter = new TestDNAWriter();
    state.dehydrate(new ObjectID(1), dnaWriter, DNAType.L1_FAULT); // Only dehydrate the fields
    Assert.assertEquals(10, dnaWriter.getActionCount()); // only the physical fields are dehydrated

    final TestDNACursor cursor3 = dnaWriter.getDNACursor();
    final ConcurrentDistributedServerMapManagedObjectState state3 = (ConcurrentDistributedServerMapManagedObjectState) createManagedObjectState(CDSM_DSO_CLASSNAME,
                                                                                                                                                cursor3);
    state3.apply(new ObjectID(1), cursor3, new ApplyTransactionInfo());
    assertTrue(state3.references.isEmpty());

  }

  private TestDNACursor createDNACursor() {
    final TestDNACursor cursor = new TestDNACursor();
    cursor.addPhysicalAction(ConcurrentDistributedMapManagedObjectState.DSO_LOCK_TYPE_FIELDNAME, new Integer(1), false);
    cursor.addPhysicalAction(ConcurrentDistributedMapManagedObjectState.LOCK_STRATEGY_FIELDNAME, new ObjectID(20),
                             false);
    cursor.addPhysicalAction(ConcurrentDistributedServerMapManagedObjectState.MAX_TTI_SECONDS_FIELDNAME,
                             Integer.valueOf(0), false);
    cursor.addPhysicalAction(ConcurrentDistributedServerMapManagedObjectState.MAX_TTL_SECONDS_FIELDNAME,
                             Integer.valueOf(0), false);
    cursor.addPhysicalAction(ConcurrentDistributedServerMapManagedObjectState.TARGET_MAX_IN_MEMORY_COUNT_FIELDNAME,
                             Integer.valueOf(0), false);
    cursor.addPhysicalAction(ConcurrentDistributedServerMapManagedObjectState.TARGET_MAX_TOTAL_COUNT_FIELDNAME,
                             Integer.valueOf(0), false);
    cursor.addPhysicalAction(ConcurrentDistributedServerMapManagedObjectState.INVALIDATE_ON_CHANGE,
                             Boolean.valueOf(false), false);
    cursor.addPhysicalAction(ConcurrentDistributedServerMapManagedObjectState.CACHE_NAME_FIELDNAME, "bob", false);
    cursor.addPhysicalAction(ConcurrentDistributedServerMapManagedObjectState.LOCAL_CACHE_ENABLED_FIELDNAME,
                             Boolean.valueOf(true), false);
    cursor.addPhysicalAction(ConcurrentDistributedServerMapManagedObjectState.DELETE_VALUE_ON_REMOVE,
                             Boolean.valueOf(true), false);

    for (int i = 0; i < 500; i++) {
      cursor.addLogicalAction(SerializationUtil.PUT, new Object[] { "key-" + i, new ObjectID(1000 + i) });
    }

    return cursor;
  }

}
