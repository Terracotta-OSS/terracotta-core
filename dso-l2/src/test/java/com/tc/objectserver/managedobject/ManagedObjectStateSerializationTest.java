/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.object.LogicalOperation;
import com.tc.object.ObjectID;
import com.tc.object.TestDNACursor;
import com.tc.objectserver.core.api.ManagedObjectState;
import com.tc.objectserver.managedobject.ManagedObjectStateStaticConfig.ToolkitTypeNames;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class ManagedObjectStateSerializationTest extends ManagedObjectStateSerializationTestBase {

  public void testCheckIfMissingAnyManagedObjectType() throws Exception {
    final Field[] fields = ManagedObjectState.class.getDeclaredFields();

    for (final Field field : fields) {
      final int fieldModifier = field.getModifiers();
      if (Modifier.isStatic(fieldModifier) && Modifier.isFinal(fieldModifier)) {
        final Byte type = (Byte) field.get(null);
        switch (type.byteValue()) {
          case ManagedObjectState.MAP_TYPE:
          case ManagedObjectState.PARTIAL_MAP_TYPE:
            // Map type is tested in another test.
            break;
          case ManagedObjectState.LIST_TYPE:
            testList();
            break;
          case ManagedObjectState.TDC_SERIALIZED_ENTRY:
            testTcHibernateSerializedEntry();
            break;
          default:
            testConcurrentDistributedServerMap();
        }
      }
    }
  }

  public void testTcHibernateSerializedEntry() throws Exception {
    final String className = ToolkitTypeNames.SERIALIZED_MAP_VALUE_TYPE;
    final TestDNACursor cursor = new TestDNACursor();

    cursor.addEntireArray(new byte[] { 1, 2, 3, 4 });

    final ManagedObjectState state = applyValidation(className, cursor);

    serializationValidation(state, cursor, ManagedObjectState.TDC_SERIALIZED_ENTRY);
  }

  public void testList() throws Exception {
    final String className = ManagedObjectStateStaticConfig.TOOLKIT_LIST.getClientClassName();
    final TestDNACursor cursor = new TestDNACursor();

    cursor.addLogicalAction(LogicalOperation.ADD, new Object[] { new ObjectID(2002) });
    cursor.addLogicalAction(LogicalOperation.ADD, new Object[] { new ObjectID(2003) });

    final ManagedObjectState state = applyValidation(className, cursor);

    serializationValidation(state, cursor, ManagedObjectState.LIST_TYPE);
  }

  public void testConcurrentDistributedServerMap() throws Exception {
    final String className = ManagedObjectStateStaticConfig.ToolkitTypeNames.SERVER_MAP_TYPE;
    final TestDNACursor cursor = new TestDNACursor();

    cursor.addPhysicalAction(ConcurrentDistributedServerMapManagedObjectState.LOCK_TYPE_FIELDNAME, new Integer(42),
                             false);
    cursor.addPhysicalAction(ConcurrentDistributedServerMapManagedObjectState.MAX_TTI_SECONDS_FIELDNAME,
                             Integer.valueOf(0), false);
    cursor.addPhysicalAction(ConcurrentDistributedServerMapManagedObjectState.MAX_TTL_SECONDS_FIELDNAME,
                             Integer.valueOf(0), false);
    cursor.addPhysicalAction(ConcurrentDistributedServerMapManagedObjectState.MAX_COUNT_IN_CLUSTER_FIELDNAME,
                             Integer.valueOf(0), false);
    cursor.addPhysicalAction(ConcurrentDistributedServerMapManagedObjectState.INVALIDATE_ON_CHANGE_FIELDNAME,
                             Boolean.valueOf(false), false);
    cursor.addPhysicalAction(ConcurrentDistributedServerMapManagedObjectState.CACHE_NAME_FIELDNAME, "cash name", false);
    cursor.addPhysicalAction(ConcurrentDistributedServerMapManagedObjectState.LOCAL_CACHE_ENABLED_FIELDNAME,
                             Boolean.valueOf(true), false);
    cursor.addPhysicalAction(ConcurrentDistributedServerMapManagedObjectState.COMPRESSION_ENABLED_FIELDNAME,
                             Boolean.valueOf(false), false);
    cursor.addPhysicalAction(ConcurrentDistributedServerMapManagedObjectState.COPY_ON_READ_ENABLED_FIELDNAME,
                             Boolean.valueOf(false), false);
    cursor.addPhysicalAction(ConcurrentDistributedServerMapManagedObjectState.EVICTION_ENABLED_FIELDNAME,
                             Boolean.valueOf(false), false);

    cursor.addLogicalAction(LogicalOperation.PUT, new Object[] { new ObjectID(2001), new ObjectID(2003), 0L, 0L, 0L, 0L });
    cursor.addLogicalAction(LogicalOperation.PUT, new Object[] { new ObjectID(2002), new ObjectID(2004), 0L, 0L, 0L, 0L });

    final ManagedObjectState state = applyValidation(className, cursor);

    serializationValidation(state, cursor, ManagedObjectStateStaticConfig.SERVER_MAP.getStateObjectType());
  }

}
