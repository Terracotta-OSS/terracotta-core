/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.object.ObjectID;
import com.tc.object.SerializationUtil;
import com.tc.object.TestDNACursor;
import com.tc.object.dna.impl.ClassLoaderInstance;
import com.tc.object.dna.impl.UTF8ByteDataHolder;
import com.tc.objectserver.core.api.ManagedObjectState;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ManagedObjectStateSerializationTest extends ManagedObjectStateSerializationTestBase {

  public void testCheckIfMissingAnyManagedObjectType() throws Exception {
    final Field[] fields = ManagedObjectState.class.getDeclaredFields();

    for (final Field field : fields) {
      final int fieldModifier = field.getModifiers();
      if (Modifier.isStatic(fieldModifier) && Modifier.isFinal(fieldModifier)) {
        final Byte type = (Byte) field.get(null);
        switch (type.byteValue()) {
          case ManagedObjectState.PHYSICAL_TYPE:
            testPhysical();
            break;
          case ManagedObjectState.DATE_TYPE:
            testDate();
            break;
          case ManagedObjectState.MAP_TYPE:
          case ManagedObjectState.PARTIAL_MAP_TYPE:
            // Map type is tested in another test.
            break;
          case ManagedObjectState.LINKED_HASHMAP_TYPE:
            testLinkedHashMap();
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
          case ManagedObjectState.LINKED_LIST_TYPE:
            testLinkedList();
            break;
          case ManagedObjectState.LINKED_HASHSET_TYPE:
            testLinkedHashSet();
            break;
          case ManagedObjectState.SET_TYPE:
            testSet();
            break;
          case ManagedObjectState.TREE_SET_TYPE:
            testTreeSet();
            break;
          case ManagedObjectState.TREE_MAP_TYPE:
            testTreeMap();
            break;
          case ManagedObjectState.QUEUE_TYPE:
            testLinkedBlockingQueue();
            break;
          case ManagedObjectState.CONCURRENT_HASHMAP_TYPE:
            testConcurrentHashMap();
            break;
          case ManagedObjectState.URL_TYPE:
            testURL();
            break;
          case ManagedObjectState.CONCURRENT_DISTRIBUTED_MAP_TYPE:
            testConcurrentDistributedMap();
            break;
          case ManagedObjectState.CONCURRENT_DISTRIBUTED_SERVER_MAP_TYPE:
            testConcurrentDistributedServerMap();
            break;
          case ManagedObjectState.TDC_SERIALIZED_ENTRY:
            testTcHibernateSerializedEntry();
            break;
          case ManagedObjectState.TDC_CUSTOM_LIFESPAN_SERIALIZED_ENTRY:
            testTcHibernateCustomSerializedEntry();
            break;
          default:
            throw new AssertionError("Type " + type
                                     + " does not have a test case in ManagedObjectStateSerializationTest.");
        }
      }
    }
  }

  public void testTcHibernateSerializedEntry() throws Exception {
    final String className = TDCSerializedEntryManagedObjectState.SERIALIZED_ENTRY;
    final TestDNACursor cursor = new TestDNACursor();

    cursor.addPhysicalAction(TDCSerializedEntryManagedObjectState.CREATE_TIME_FIELD, new Integer(1), false);
    cursor.addPhysicalAction(TDCSerializedEntryManagedObjectState.LAST_ACCESS_TIME_FIELD, new Integer(2), false);
    cursor.addEntireArray(new byte[] { 1, 2, 3, 4 });

    final ManagedObjectState state = applyValidation(className, cursor);

    serializationValidation(state, cursor, ManagedObjectState.TDC_SERIALIZED_ENTRY);
  }

  public void testTcHibernateCustomSerializedEntry() throws Exception {
    final String className = TDCCustomLifespanSerializedEntryManagedObjectState.CUSTOM_SERIALIZED_ENTRY;
    final TestDNACursor cursor = new TestDNACursor();

    cursor.addPhysicalAction(TDCSerializedEntryManagedObjectState.CREATE_TIME_FIELD, new Integer(1), false);
    cursor.addPhysicalAction(TDCSerializedEntryManagedObjectState.LAST_ACCESS_TIME_FIELD, new Integer(2), false);
    cursor.addEntireArray(new byte[] { 1, 2, 3, 4 });
    cursor
        .addPhysicalAction(TDCCustomLifespanSerializedEntryManagedObjectState.CUSTOM_TTI_FIELD, new Integer(3), false);
    cursor
        .addPhysicalAction(TDCCustomLifespanSerializedEntryManagedObjectState.CUSTOM_TTL_FIELD, new Integer(4), false);

    final ManagedObjectState state = applyValidation(className, cursor);

    serializationValidation(state, cursor, ManagedObjectState.TDC_CUSTOM_LIFESPAN_SERIALIZED_ENTRY);
  }

  public void testProxy() throws Exception {
    final String CLASSLOADER_FIELD_NAME = "java.lang.reflect.Proxy.loader";
    final String INTERFACES_FIELD_NAME = "java.lang.reflect.Proxy.interfaces";
    final String INVOCATION_HANDLER_FIELD_NAME = "java.lang.reflect.Proxy.h";

    final MyInvocationHandler handler = new MyInvocationHandler();
    final Proxy myProxy = (Proxy) Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[] {
        MyProxyInf1.class, MyProxyInf2.class }, handler);
    final String className = myProxy.getClass().getName();

    final TestDNACursor cursor = new TestDNACursor();

    cursor.addPhysicalAction(CLASSLOADER_FIELD_NAME, new ClassLoaderInstance(new UTF8ByteDataHolder("loader desc")),
                             true);
    cursor.addPhysicalAction(INTERFACES_FIELD_NAME, myProxy.getClass().getInterfaces(), true);
    cursor.addPhysicalAction(INVOCATION_HANDLER_FIELD_NAME, new ObjectID(2002), true);

    final ManagedObjectState state = applyValidation(className, cursor);

    serializationValidation(state, cursor, ManagedObjectState.PHYSICAL_TYPE);
  }

  public void testPhysical() throws Exception {
    final String className = "com.tc.objectserver.managedobject.ManagedObjectStateSerializationTest";
    final TestDNACursor cursor = new TestDNACursor();

    cursor.addPhysicalAction("field1", new ObjectID(2002), true);
    cursor.addPhysicalAction("field2", new ObjectID(2003), true);
    cursor.addPhysicalAction("field3", new Integer(33), false);

    final ManagedObjectState state = applyValidation(className, cursor);

    serializationValidation(state, cursor, ManagedObjectState.PHYSICAL_TYPE);
  }

  public void testDate() throws Exception {
    final String className = "java.util.Date";

    final TestDNACursor cursor = new TestDNACursor();

    cursor.addLogicalAction(SerializationUtil.SET_TIME, new Long[] { new Long(System.currentTimeMillis()) });
    cursor.addLogicalAction(SerializationUtil.SET_NANOS, new Integer[] { new Integer(0) });

    final ManagedObjectState state = applyValidation(className, cursor);

    serializationValidation(state, cursor, ManagedObjectState.DATE_TYPE);
  }

  public void testLinkedHashMap() throws Exception {
    final String className = "java.util.LinkedHashMap";
    final String ACCESS_ORDER_FIELDNAME = "java.util.LinkedHashMap.accessOrder";

    final TestDNACursor cursor = new TestDNACursor();

    cursor.addPhysicalAction(ACCESS_ORDER_FIELDNAME, Boolean.FALSE, false);

    cursor.addLogicalAction(SerializationUtil.PUT, new Object[] { new ObjectID(2002), new ObjectID(2003) });
    cursor.addLogicalAction(SerializationUtil.PUT, new Object[] { new ObjectID(2004), new ObjectID(2005) });

    final ManagedObjectState state = applyValidation(className, cursor);

    serializationValidation(state, cursor, ManagedObjectState.LINKED_HASHMAP_TYPE);
  }

  public void testArray() throws Exception {
    final String className = "[java.lang.Integer";
    final TestDNACursor cursor = new TestDNACursor();

    cursor.addArrayAction(new Integer[] { new Integer(27) });

    final ManagedObjectState state = applyValidation(className, cursor);

    serializationValidation(state, cursor, ManagedObjectState.ARRAY_TYPE);
  }

  public void testLiteral() throws Exception {
    final String className = "java.lang.Integer";
    final TestDNACursor cursor = new TestDNACursor();

    cursor.addLiteralAction(new Integer(27));

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

  public void testLinkedList() throws Exception {
    final String className = "java.util.LinkedList";
    final TestDNACursor cursor = new TestDNACursor();

    cursor.addLogicalAction(SerializationUtil.ADD, new Object[] { new ObjectID(2002) });
    cursor.addLogicalAction(SerializationUtil.ADD, new Object[] { new ObjectID(2003) });

    final ManagedObjectState state = applyValidation(className, cursor);

    serializationValidation(state, cursor, ManagedObjectState.LINKED_LIST_TYPE);
  }

  public void testSet() throws Exception {
    final String className = "java.util.HashSet";
    final TestDNACursor cursor = new TestDNACursor();

    cursor.addLogicalAction(SerializationUtil.ADD, new Object[] { new ObjectID(2002) });
    cursor.addLogicalAction(SerializationUtil.ADD, new Object[] { new ObjectID(2003) });

    final ManagedObjectState state = applyValidation(className, cursor);

    serializationValidation(state, cursor, ManagedObjectState.SET_TYPE);
  }

  public void testLinkedHashSet() throws Exception {
    final String className = "java.util.LinkedHashSet";
    final TestDNACursor cursor = new TestDNACursor();

    cursor.addLogicalAction(SerializationUtil.ADD, new Object[] { new ObjectID(2002) });
    cursor.addLogicalAction(SerializationUtil.ADD, new Object[] { new ObjectID(2003) });

    final ManagedObjectState state = applyValidation(className, cursor);

    serializationValidation(state, cursor, ManagedObjectState.LINKED_HASHSET_TYPE);
  }

  public void testTreeSet() throws Exception {
    final String className = "java.util.TreeSet";
    final String COMPARATOR_FIELDNAME = "java.util.TreeMap.comparator";

    final TestDNACursor cursor = new TestDNACursor();

    cursor.addPhysicalAction(COMPARATOR_FIELDNAME, new ObjectID(2001), true);

    cursor.addLogicalAction(SerializationUtil.ADD, new Object[] { new ObjectID(2002) });
    cursor.addLogicalAction(SerializationUtil.ADD, new Object[] { new ObjectID(2003) });

    final ManagedObjectState state = applyValidation(className, cursor);

    serializationValidation(state, cursor, ManagedObjectState.TREE_SET_TYPE);
  }

  public void testTreeMap() throws Exception {
    final String className = "java.util.TreeMap";
    final String COMPARATOR_FIELDNAME = "java.util.TreeMap.comparator";

    final TestDNACursor cursor = new TestDNACursor();

    cursor.addPhysicalAction(COMPARATOR_FIELDNAME, new ObjectID(2001), true);

    cursor.addLogicalAction(SerializationUtil.PUT, new Object[] { new ObjectID(2002), new ObjectID(2003) });
    cursor.addLogicalAction(SerializationUtil.PUT, new Object[] { new ObjectID(2004), new ObjectID(2005) });

    final ManagedObjectState state = applyValidation(className, cursor);

    serializationValidation(state, cursor, ManagedObjectState.TREE_MAP_TYPE);
  }

  public void testLinkedBlockingQueue() throws Exception {
    final String className = "java.util.concurrent.LinkedBlockingQueue";
    final String TAKE_LOCK_FIELD_NAME = "takeLock";
    final String PUT_LOCK_FIELD_NAME = "putLock";
    final String CAPACITY_FIELD_NAME = "capacity";

    final TestDNACursor cursor = new TestDNACursor();

    cursor.addPhysicalAction(TAKE_LOCK_FIELD_NAME, new ObjectID(2001), true);
    cursor.addPhysicalAction(PUT_LOCK_FIELD_NAME, new ObjectID(2002), true);
    cursor.addPhysicalAction(CAPACITY_FIELD_NAME, new Integer(100), false);

    cursor.addLogicalAction(SerializationUtil.PUT, new Object[] { new ObjectID(2003) });
    cursor.addLogicalAction(SerializationUtil.PUT, new Object[] { new ObjectID(2004) });

    final ManagedObjectState state = applyValidation(className, cursor);

    serializationValidation(state, cursor, ManagedObjectState.QUEUE_TYPE);
  }

  public void testConcurrentHashMap() throws Exception {
    final String className = ConcurrentHashMap.class.getName();
    final TestDNACursor cursor = new TestDNACursor();

    cursor.addPhysicalAction(ConcurrentHashMapManagedObjectState.SEGMENT_MASK_FIELD_NAME, new Integer(10), false);
    cursor.addPhysicalAction(ConcurrentHashMapManagedObjectState.SEGMENT_SHIFT_FIELD_NAME, new Integer(20), false);
    final ObjectID[] segments = new ObjectID[] { new ObjectID(2001), new ObjectID(2002) };
    cursor.addArrayAction(segments);

    cursor.addLogicalAction(SerializationUtil.PUT, new Object[] { new ObjectID(2002), new ObjectID(2003) });
    cursor.addLogicalAction(SerializationUtil.PUT, new Object[] { new ObjectID(2004), new ObjectID(2005) });

    final ManagedObjectState state = applyValidation(className, cursor);

    serializationValidation(state, cursor, ManagedObjectState.CONCURRENT_HASHMAP_TYPE);
  }

  public void testURL() throws Exception {
    final String className = "java.net.URL";

    final TestDNACursor cursor = new TestDNACursor();

    cursor.addLogicalAction(SerializationUtil.URL_SET, new Object[] { "http", "terracotta.org", new Integer(8080),
        "auth", "user:pass", "/test", "par1=val1", "ref" });

    final ManagedObjectState state = applyValidation(className, cursor);

    serializationValidation(state, cursor, ManagedObjectState.URL_TYPE);
  }

  // XXX: This is a rather ugly hack to get around the requirements of tim-concurrent-collections.
  public void testConcurrentDistributedMap() throws Exception {
    final String className = "com.terracotta.toolkit.collections.ConcurrentDistributedMapDso";
    final TestDNACursor cursor = new TestDNACursor();

    cursor
        .addPhysicalAction(ConcurrentDistributedMapManagedObjectState.DSO_LOCK_TYPE_FIELDNAME, new Integer(42), false);
    cursor.addPhysicalAction(ConcurrentDistributedMapManagedObjectState.LOCK_STRATEGY_FIELDNAME, new ObjectID(1, 12),
                             true);

    cursor.addLogicalAction(SerializationUtil.PUT, new Object[] { new ObjectID(2001), new ObjectID(2003) });
    cursor.addLogicalAction(SerializationUtil.PUT, new Object[] { new ObjectID(2002), new ObjectID(2004) });

    final ManagedObjectState state = applyValidation(className, cursor);

    serializationValidation(state, cursor, ManagedObjectState.CONCURRENT_DISTRIBUTED_MAP_TYPE);
  }

  public void testConcurrentDistributedServerMap() throws Exception {
    final String className = "com.terracotta.toolkit.collections.ConcurrentDistributedServerMapDso";
    final TestDNACursor cursor = new TestDNACursor();

    cursor
        .addPhysicalAction(ConcurrentDistributedMapManagedObjectState.DSO_LOCK_TYPE_FIELDNAME, new Integer(42), false);
    cursor.addPhysicalAction(ConcurrentDistributedMapManagedObjectState.LOCK_STRATEGY_FIELDNAME, new ObjectID(1, 12),
                             true);
    cursor.addPhysicalAction(ConcurrentDistributedServerMapManagedObjectState.MAX_TTI_SECONDS_FIELDNAME,
                             new Integer(0), false);
    cursor.addPhysicalAction(ConcurrentDistributedServerMapManagedObjectState.MAX_TTL_SECONDS_FIELDNAME,
                             new Integer(0), false);
    cursor.addPhysicalAction(ConcurrentDistributedServerMapManagedObjectState.TARGET_MAX_IN_MEMORY_COUNT_FIELDNAME,
                             new Integer(0), false);
    cursor.addPhysicalAction(ConcurrentDistributedServerMapManagedObjectState.TARGET_MAX_TOTAL_COUNT_FIELDNAME,
                             new Integer(0), false);
    cursor.addPhysicalAction(ConcurrentDistributedServerMapManagedObjectState.INVALIDATE_ON_CHANGE, new Boolean(false),
                             false);
    cursor.addPhysicalAction(ConcurrentDistributedServerMapManagedObjectState.CACHE_NAME_FIELDNAME, "cash name", false);

    cursor.addLogicalAction(SerializationUtil.PUT, new Object[] { new ObjectID(2001), new ObjectID(2003) });
    cursor.addLogicalAction(SerializationUtil.PUT, new Object[] { new ObjectID(2002), new ObjectID(2004) });

    final ManagedObjectState state = applyValidation(className, cursor);

    serializationValidation(state, cursor, ManagedObjectState.CONCURRENT_DISTRIBUTED_SERVER_MAP_TYPE);
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
      } else if (method.getName().equals("hashCode")) { return new Integer(System.identityHashCode(proxy)); }
      return null;
    }
  }

}
