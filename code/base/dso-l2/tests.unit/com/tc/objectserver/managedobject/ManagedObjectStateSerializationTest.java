/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.object.ObjectID;
import com.tc.object.SerializationUtil;
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
    Field[] fields = ManagedObjectState.class.getDeclaredFields();

    for (int i = 0; i < fields.length; i++) {
      Field field = fields[i];

      int fieldModifier = field.getModifiers();
      if (Modifier.isStatic(fieldModifier) && Modifier.isFinal(fieldModifier)) {
        Byte type = (Byte) field.get(null);
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
          default:
            throw new AssertionError("Type " + type
                                     + " does not have a test case in ManagedObjectStateSerializationTest.");
        }
      }
    }
  }

  public void testProxy() throws Exception {
    String CLASSLOADER_FIELD_NAME = "java.lang.reflect.Proxy.loader";
    String INTERFACES_FIELD_NAME = "java.lang.reflect.Proxy.interfaces";
    String INVOCATION_HANDLER_FIELD_NAME = "java.lang.reflect.Proxy.h";

    MyInvocationHandler handler = new MyInvocationHandler();
    Proxy myProxy = (Proxy) Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[] { MyProxyInf1.class,
        MyProxyInf2.class }, handler);
    String className = myProxy.getClass().getName();

    TestDNACursor cursor = new TestDNACursor();

    cursor.addPhysicalAction(CLASSLOADER_FIELD_NAME, new ClassLoaderInstance(new UTF8ByteDataHolder("loader desc")), true);
    cursor.addPhysicalAction(INTERFACES_FIELD_NAME, myProxy.getClass().getInterfaces(), true);
    cursor.addPhysicalAction(INVOCATION_HANDLER_FIELD_NAME, new ObjectID(2002), true);

    ManagedObjectState state = applyValidation(className, cursor);

    serializationValidation(state, cursor, ManagedObjectState.PHYSICAL_TYPE);
  }

  public void testPhysical() throws Exception {
    String className = "com.tc.objectserver.managedobject.ManagedObjectStateSerializationTest";
    TestDNACursor cursor = new TestDNACursor();

    cursor.addPhysicalAction("field1", new ObjectID(2002), true);
    cursor.addPhysicalAction("field2", new ObjectID(2003), true);
    cursor.addPhysicalAction("field3", new Integer(33), false);

    ManagedObjectState state = applyValidation(className, cursor);

    serializationValidation(state, cursor, ManagedObjectState.PHYSICAL_TYPE);
  }

  public void testDate() throws Exception {
    String className = "java.util.Date";

    TestDNACursor cursor = new TestDNACursor();

    cursor.addLogicalAction(SerializationUtil.SET_TIME, new Long[] { new Long(System.currentTimeMillis()) });
    cursor.addLogicalAction(SerializationUtil.SET_NANOS, new Integer[] { new Integer(0) });

    ManagedObjectState state = applyValidation(className, cursor);

    serializationValidation(state, cursor, ManagedObjectState.DATE_TYPE);
  }

  public void testLinkedHashMap() throws Exception {
    String className = "java.util.LinkedHashMap";
    String ACCESS_ORDER_FIELDNAME = "java.util.LinkedHashMap.accessOrder";

    TestDNACursor cursor = new TestDNACursor();

    cursor.addPhysicalAction(ACCESS_ORDER_FIELDNAME, Boolean.FALSE, false);

    cursor.addLogicalAction(SerializationUtil.PUT, new Object[] { new ObjectID(2002), new ObjectID(2003) });
    cursor.addLogicalAction(SerializationUtil.PUT, new Object[] { new ObjectID(2004), new ObjectID(2005) });

    ManagedObjectState state = applyValidation(className, cursor);

    serializationValidation(state, cursor, ManagedObjectState.LINKED_HASHMAP_TYPE);
  }

  public void testArray() throws Exception {
    String className = "[java.lang.Integer";
    TestDNACursor cursor = new TestDNACursor();

    cursor.addArrayAction(new Integer[] { new Integer(27) });

    ManagedObjectState state = applyValidation(className, cursor);

    serializationValidation(state, cursor, ManagedObjectState.ARRAY_TYPE);
  }

  public void testLiteral() throws Exception {
    String className = "java.lang.Integer";
    TestDNACursor cursor = new TestDNACursor();

    cursor.addLiteralAction(new Integer(27));

    ManagedObjectState state = applyValidation(className, cursor);

    serializationValidation(state, cursor, ManagedObjectState.LITERAL_TYPE);
  }

  public void testList() throws Exception {
    String className = "java.util.ArrayList";
    TestDNACursor cursor = new TestDNACursor();

    cursor.addLogicalAction(SerializationUtil.ADD, new Object[] { new ObjectID(2002) });
    cursor.addLogicalAction(SerializationUtil.ADD, new Object[] { new ObjectID(2003) });

    ManagedObjectState state = applyValidation(className, cursor);

    serializationValidation(state, cursor, ManagedObjectState.LIST_TYPE);
  }

  public void testSet() throws Exception {
    String className = "java.util.HashSet";
    TestDNACursor cursor = new TestDNACursor();

    cursor.addLogicalAction(SerializationUtil.ADD, new Object[] { new ObjectID(2002) });
    cursor.addLogicalAction(SerializationUtil.ADD, new Object[] { new ObjectID(2003) });

    ManagedObjectState state = applyValidation(className, cursor);

    serializationValidation(state, cursor, ManagedObjectState.SET_TYPE);
  }

  public void testTreeSet() throws Exception {
    String className = "java.util.TreeSet";
    String COMPARATOR_FIELDNAME = "java.util.TreeMap.comparator";

    TestDNACursor cursor = new TestDNACursor();

    cursor.addPhysicalAction(COMPARATOR_FIELDNAME, new ObjectID(2001), true);

    cursor.addLogicalAction(SerializationUtil.ADD, new Object[] { new ObjectID(2002) });
    cursor.addLogicalAction(SerializationUtil.ADD, new Object[] { new ObjectID(2003) });

    ManagedObjectState state = applyValidation(className, cursor);

    serializationValidation(state, cursor, ManagedObjectState.TREE_SET_TYPE);
  }

  public void testTreeMap() throws Exception {
    String className = "java.util.TreeMap";
    String COMPARATOR_FIELDNAME = "java.util.TreeMap.comparator";

    TestDNACursor cursor = new TestDNACursor();

    cursor.addPhysicalAction(COMPARATOR_FIELDNAME, new ObjectID(2001), true);

    cursor.addLogicalAction(SerializationUtil.PUT, new Object[] { new ObjectID(2002), new ObjectID(2003) });
    cursor.addLogicalAction(SerializationUtil.PUT, new Object[] { new ObjectID(2004), new ObjectID(2005) });

    ManagedObjectState state = applyValidation(className, cursor);

    serializationValidation(state, cursor, ManagedObjectState.TREE_MAP_TYPE);
  }

  public void testLinkedBlockingQueue() throws Exception {
    String className = "java.util.concurrent.LinkedBlockingQueue";
    String TAKE_LOCK_FIELD_NAME = "java.util.concurrent.LinkedBlockingQueue.takeLock";
    String PUT_LOCK_FIELD_NAME = "java.util.concurrent.LinkedBlockingQueue.putLock";
    String CAPACITY_FIELD_NAME = "java.util.concurrent.LinkedBlockingQueue.capacity";

    TestDNACursor cursor = new TestDNACursor();

    cursor.addPhysicalAction(TAKE_LOCK_FIELD_NAME, new ObjectID(2001), true);
    cursor.addPhysicalAction(PUT_LOCK_FIELD_NAME, new ObjectID(2002), true);
    cursor.addPhysicalAction(CAPACITY_FIELD_NAME, new Integer(100), false);

    cursor.addLogicalAction(SerializationUtil.PUT, new Object[] { new ObjectID(2003) });
    cursor.addLogicalAction(SerializationUtil.PUT, new Object[] { new ObjectID(2004) });

    ManagedObjectState state = applyValidation(className, cursor);

    serializationValidation(state, cursor, ManagedObjectState.QUEUE_TYPE);
  }

  public void testConcurrentHashMap() throws Exception {
    String className = ConcurrentHashMap.class.getName();
    TestDNACursor cursor = new TestDNACursor();

    cursor.addPhysicalAction(ConcurrentHashMapManagedObjectState.SEGMENT_MASK_FIELD_NAME, new Integer(10), false);
    cursor.addPhysicalAction(ConcurrentHashMapManagedObjectState.SEGMENT_SHIFT_FIELD_NAME, new Integer(20), false);
    ObjectID[] segments = new ObjectID[] { new ObjectID(2001), new ObjectID(2002) };
    cursor.addArrayAction(segments);

    cursor.addLogicalAction(SerializationUtil.PUT, new Object[] { new ObjectID(2002), new ObjectID(2003) });
    cursor.addLogicalAction(SerializationUtil.PUT, new Object[] { new ObjectID(2004), new ObjectID(2005) });

    ManagedObjectState state = applyValidation(className, cursor);

    serializationValidation(state, cursor, ManagedObjectState.CONCURRENT_HASHMAP_TYPE);
  }

  public void testURL() throws Exception {
    String className = "java.net.URL";

    TestDNACursor cursor = new TestDNACursor();

    cursor.addLogicalAction(SerializationUtil.URL_SET, new Object[] { "http", "terracotta.org", new Integer(8080),
        "auth", "user:pass", "/test", "par1=val1", "ref" });

    ManagedObjectState state = applyValidation(className, cursor);

    serializationValidation(state, cursor, ManagedObjectState.URL_TYPE);
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
    private Map values       = new HashMap();
    private Map stringValues = new HashMap();

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      if (method.getName().equals("getValue")) {
        return values.get(proxy);
      } else if (method.getName().equals("setValue")) {
        values.put(proxy, args[0]);
        return null;
      } else if (method.getName().equals("setStringValue")) {
        stringValues.put(proxy, args[0]);
        return null;
      } else if (method.getName().equals("getStringValue")) {
        return stringValues.get(proxy);
      } else if (method.getName().equals("hashCode")) { return new Integer(System.identityHashCode(proxy)); }
      return null;
    }
  }

}
