/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.object.ObjectID;
import com.tc.object.SerializationUtil;
import com.tc.object.TestDNACursor;
import com.tc.objectserver.core.api.ManagedObjectState;
import com.tc.objectserver.managedobject.ManagedObjectStateStaticConfig.ToolkitTypeNames;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

public class ManagedObjectStateSerializationTest extends ManagedObjectStateSerializationTestBase {

  static {
    ManagedObjectStateFactory.enableLegacyTypes();
  }

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
          case ManagedObjectState.ARRAY_TYPE:
            testArray();
            break;
          case ManagedObjectState.LITERAL_TYPE:
            testLiteral();
            break;
          case ManagedObjectState.LIST_TYPE:
            testList();
            break;
          case ManagedObjectState.SET_TYPE:
            testSet();
            break;
          case ManagedObjectState.TDC_SERIALIZED_ENTRY:
            testTcHibernateSerializedEntry();
            break;
          case ManagedObjectState.TDC_CUSTOM_LIFESPAN_SERIALIZED_ENTRY:
            testTcHibernateCustomSerializedEntry();
            break;
          default:
            testConcurrentDistributedServerMap();
        }
      }
    }
  }

  public void testTcHibernateSerializedEntry() throws Exception {
    final String className = ToolkitTypeNames.SERIALIZED_ENTRY_TYPE;
    final TestDNACursor cursor = new TestDNACursor();

    cursor.addPhysicalAction(TDCSerializedEntryManagedObjectState.CREATE_TIME_FIELD, new Integer(1), false);
    cursor.addPhysicalAction(TDCSerializedEntryManagedObjectState.LAST_ACCESS_TIME_FIELD, new Integer(2), false);
    cursor.addEntireArray(new byte[] { 1, 2, 3, 4 });

    final ManagedObjectState state = applyValidation(className, cursor);

    serializationValidation(state, cursor, ManagedObjectState.TDC_SERIALIZED_ENTRY);
  }

  public void testTcHibernateCustomSerializedEntry() throws Exception {
    final String className = ManagedObjectStateStaticConfig.CUSTOM_LIFESPAN_SERIALIZED_ENTRY.getClientClassName();
    final TestDNACursor cursor = new TestDNACursor();

    cursor.addPhysicalAction(TDCSerializedEntryManagedObjectState.CREATE_TIME_FIELD, Integer.valueOf(1), false);
    cursor.addPhysicalAction(TDCSerializedEntryManagedObjectState.LAST_ACCESS_TIME_FIELD, Integer.valueOf(2), false);
    cursor.addEntireArray(new byte[] { 1, 2, 3, 4 });
    cursor.addPhysicalAction(TDCCustomLifespanSerializedEntryManagedObjectState.CUSTOM_TTI_FIELD, Integer.valueOf(3),
                             false);
    cursor.addPhysicalAction(TDCCustomLifespanSerializedEntryManagedObjectState.CUSTOM_TTL_FIELD, Integer.valueOf(4),
                             false);

    final ManagedObjectState state = applyValidation(className, cursor);

    serializationValidation(state, cursor, ManagedObjectState.TDC_CUSTOM_LIFESPAN_SERIALIZED_ENTRY);
  }

  public void testArray() throws Exception {
    final String className = "[java.lang.Integer";
    final TestDNACursor cursor = new TestDNACursor();

    cursor.addArrayAction(new Integer[] { Integer.valueOf(27) });

    final ManagedObjectState state = applyValidation(className, cursor);

    serializationValidation(state, cursor, ManagedObjectState.ARRAY_TYPE);
  }

  public void testLiteral() throws Exception {
    final String className = "java.lang.Integer";
    final TestDNACursor cursor = new TestDNACursor();

    cursor.addLiteralAction(Integer.valueOf(27));

    final ManagedObjectState state = applyValidation(className, cursor);

    serializationValidation(state, cursor, ManagedObjectState.LITERAL_TYPE);
  }

  public void testList() throws Exception {
    final String className = "java.util.ArrayList";
    final TestDNACursor cursor = new TestDNACursor();

    cursor.addLogicalAction(SerializationUtil.ADD, new Object[] { new ObjectID(2002) });
    cursor.addLogicalAction(SerializationUtil.ADD, new Object[] { new ObjectID(2003) });

    final ManagedObjectState state = applyValidation(className, cursor);

    serializationValidation(state, cursor, ManagedObjectState.LIST_TYPE);
  }

  public void testSet() throws Exception {
    final String className = "java.util.HashSet";
    final TestDNACursor cursor = new TestDNACursor();

    cursor.addLogicalAction(SerializationUtil.ADD, new Object[] { new ObjectID(2002) });
    cursor.addLogicalAction(SerializationUtil.ADD, new Object[] { new ObjectID(2003) });

    final ManagedObjectState state = applyValidation(className, cursor);

    serializationValidation(state, cursor, ManagedObjectState.SET_TYPE);
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

    cursor.addLogicalAction(SerializationUtil.PUT, new Object[] { new ObjectID(2001), new ObjectID(2003) });
    cursor.addLogicalAction(SerializationUtil.PUT, new Object[] { new ObjectID(2002), new ObjectID(2004) });

    final ManagedObjectState state = applyValidation(className, cursor);

    serializationValidation(state, cursor, ManagedObjectStateStaticConfig.SERVER_MAP.getStateObjectType());
  }

  public void testEnum() throws Exception {
    final String className = "java.lang.Enum";
    final State state = State.RUN;
    final TestDNACursor cursor = new TestDNACursor();

    cursor.addLiteralAction(state);
    final ManagedObjectState managedObjectState = applyValidation(className, cursor);
    serializationValidation(managedObjectState, cursor, ManagedObjectState.LITERAL_TYPE);
  }

  public interface EnumIntf {
    public int getStateNum();

    public void setStateNum(int stateNum);
  }

  public enum State implements EnumIntf {
    START(0), RUN(1), STOP(2);

    private int stateNum;

    State(final int stateNum) {
      this.stateNum = stateNum;
    }

    public int getStateNum() {
      return this.stateNum;
    }

    public void setStateNum(final int stateNum) {
      this.stateNum = stateNum;
    }
  }

  public interface MyProxyInf1 {
    public int getValue();

    public void setValue(int i);
  }

  public interface MyProxyInf2 {
    public String getStringValue();

    public void setStringValue(String str);
  }

  public static class MyInvocationHandler implements InvocationHandler {
    private final Map values       = new HashMap();
    private final Map stringValues = new HashMap();

    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
      if (method.getName().equals("getValue")) {
        return this.values.get(proxy);
      } else if (method.getName().equals("setValue")) {
        this.values.put(proxy, args[0]);
        return null;
      } else if (method.getName().equals("setStringValue")) {
        this.stringValues.put(proxy, args[0]);
        return null;
      } else if (method.getName().equals("getStringValue")) {
        return this.stringValues.get(proxy);
      } else if (method.getName().equals("hashCode")) { return Integer.valueOf(System.identityHashCode(proxy)); }
      return null;
    }
  }

}
