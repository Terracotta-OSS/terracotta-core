/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.object.MockRemoteSearchRequestManager;
import com.tc.object.MockTCObject;
import com.tc.object.SerializationUtil;
import com.tc.object.TestClientObjectManager;
import com.tc.object.bytecode.hook.DSOContext;
import com.tc.object.bytecode.hook.impl.ClassProcessorHelper;
import com.tc.object.bytecode.hook.impl.DSOContextImpl;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.locks.MockClientLockManager;
import com.tc.object.tx.MockTransactionManager;
import com.tc.util.runtime.Vm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;
import java.util.Vector;

public class LogicalClassAdapterTest extends ClassAdapterTestBase {

  private TestClientObjectManager objManager;
  private Class                   clazz;
  private Object                  instance;
  private MockTCObject            tcObject;
  private List                    history;
  private MockTCObject.MethodCall call;
  private Object[]                params;

  public LogicalClassAdapterTest() {
    //
  }

  public void setUp() throws Exception {
    objManager = new TestClientObjectManager();
    objManager.setIsManaged(true);

    DSOClientConfigHelper config = configHelper();

    DSOContext context = DSOContextImpl.createContext(config, new ManagerImpl(false, objManager,
                                                                              new MockTransactionManager(),
                                                                              new MockClientLockManager(),
                                                                              new MockRemoteSearchRequestManager(),
                                                                              config, null));

    ClassProcessorHelper.setContext(Thread.currentThread().getContextClassLoader(), context);
  }

  protected void tearDown() throws Exception {
    super.tearDown();
    objManager.setIsManaged(false);
  }

  public void testHashtable() throws Exception {
    clazz = Hashtable.class;
    instance = clazz.newInstance();

    objManager.lookupOrCreate(instance);
    invokeMethod(clazz, instance, SerializationUtil.PUT_SIGNATURE, new Class[] { Object.class, Object.class },
                 new Object[] { new Integer(1), new Integer(2) });
    invokeMethod(clazz, instance, SerializationUtil.REMOVE_KEY_SIGNATURE, new Class[] { Object.class },
                 new Object[] { new Integer(1) });
    invokeMethod(clazz, instance, SerializationUtil.CLEAR_SIGNATURE, new Class[] {}, new Object[] {});
    tcObject = (MockTCObject) objManager.lookupOrCreate(instance);
    history = tcObject.getHistory();
    assertEquals(3, history.size());

    call = (MockTCObject.MethodCall) history.get(0);
    assertEquals(SerializationUtil.PUT, call.method);
    params = call.parameters;
    assertEquals(new Integer(1), params[0]);
    assertEquals(new Integer(2), params[1]);
    assertEquals(2, params.length);

    call = (MockTCObject.MethodCall) history.get(1);
    assertEquals(SerializationUtil.REMOVE, call.method);
    params = call.parameters;
    assertEquals(new Integer(1), params[0]);
    assertEquals(1, params.length);

    call = (MockTCObject.MethodCall) history.get(2);
    assertEquals(SerializationUtil.CLEAR, call.method);
    params = call.parameters;
    assertEquals(0, params.length);
  }

  public void testHashMap() throws Exception {
    clazz = HashMap.class;
    instance = clazz.newInstance();
    objManager.lookupOrCreate(instance);
    invokeMethod(clazz, instance, SerializationUtil.PUT_SIGNATURE, new Class[] { Object.class, Object.class },
                 new Object[] { new Integer(1), new Integer(2) });
    invokeMethod(clazz, instance, SerializationUtil.REMOVE_KEY_SIGNATURE, new Class[] { Object.class },
                 new Object[] { new Integer(1) });
    invokeMethod(clazz, instance, SerializationUtil.CLEAR_SIGNATURE, new Class[] {}, new Object[] {});
    tcObject = (MockTCObject) objManager.lookupOrCreate(instance);
    history = tcObject.getHistory();
    assertEquals(3, history.size());

    call = (MockTCObject.MethodCall) history.get(0);
    assertEquals(SerializationUtil.PUT, call.method);
    params = call.parameters;
    assertEquals(new Integer(1), params[0]);
    assertEquals(new Integer(2), params[1]);
    assertEquals(2, params.length);

    call = (MockTCObject.MethodCall) history.get(1);
    assertEquals(SerializationUtil.REMOVE, call.method);
    params = call.parameters;
    assertEquals(new Integer(1), params[0]);
    assertEquals(1, params.length);

    call = (MockTCObject.MethodCall) history.get(2);
    assertEquals(SerializationUtil.CLEAR, call.method);
    params = call.parameters;
    assertEquals(0, params.length);
  }

  public void testIdentityHashMap() throws Exception {
    clazz = IdentityHashMap.class;
    instance = clazz.newInstance();
    objManager.lookupOrCreate(instance);
    invokeMethod(clazz, instance, SerializationUtil.PUT_SIGNATURE, new Class[] { Object.class, Object.class },
                 new Object[] { new Integer(1), new Integer(2) });
    invokeMethod(clazz, instance, SerializationUtil.REMOVE_KEY_SIGNATURE, new Class[] { Object.class },
                 new Object[] { new Integer(1) });
    invokeMethod(clazz, instance, SerializationUtil.CLEAR_SIGNATURE, new Class[] {}, new Object[] {});
    tcObject = (MockTCObject) objManager.lookupOrCreate(instance);
    history = tcObject.getHistory();
    assertEquals(3, history.size());

    call = (MockTCObject.MethodCall) history.get(0);
    assertEquals(SerializationUtil.PUT, call.method);
    params = call.parameters;
    assertEquals(new Integer(1), params[0]);
    assertEquals(new Integer(2), params[1]);
    assertEquals(2, params.length);

    call = (MockTCObject.MethodCall) history.get(1);
    assertEquals(SerializationUtil.REMOVE, call.method);
    params = call.parameters;
    assertEquals(1, params.length);
    assertEquals(new Integer(1), params[0]);

    call = (MockTCObject.MethodCall) history.get(2);
    assertEquals(SerializationUtil.CLEAR, call.method);
    params = call.parameters;
    assertEquals(0, params.length);
  }

  public void testVector() throws Exception {
    clazz = Vector.class;
    instance = clazz.newInstance();
    objManager.lookupOrCreate(instance);
    invokeMethod(clazz, instance, SerializationUtil.ADD_SIGNATURE, new Class[] { Object.class },
                 new Object[] { new Integer(1) });
    invokeMethod(clazz, instance, SerializationUtil.ADD_AT_SIGNATURE, new Class[] { int.class, Object.class },
                 new Object[] { new Integer(0), new Integer(1) });
    LinkedList l = new LinkedList();
    l.add("Hello");
    l.add("world");
    invokeMethod(clazz, instance, SerializationUtil.ADD_ALL_AT_SIGNATURE, new Class[] { int.class, Collection.class },
                 new Object[] { new Integer(0), l });
    invokeMethod(clazz, instance, SerializationUtil.ADD_ALL_SIGNATURE, new Class[] { Collection.class },
                 new Object[] { l });
    invokeMethod(clazz, instance, SerializationUtil.REMOVE_AT_SIGNATURE, new Class[] { int.class },
                 new Object[] { new Integer(1) });
    invokeMethod(clazz, instance, SerializationUtil.SET_SIGNATURE, new Class[] { int.class, Object.class },
                 new Object[] { new Integer(1), new Integer(2) });

    // This one is very important, this test that the parameter reversing logic works
    invokeMethod(clazz, instance, SerializationUtil.SET_ELEMENT_SIGNATURE, new Class[] { Object.class, int.class },
                 new Object[] { new Integer(69), new Integer(1) });

    invokeMethod(clazz, instance, SerializationUtil.CLEAR_SIGNATURE, new Class[] {}, new Object[] {});

    tcObject = (MockTCObject) objManager.lookupOrCreate(instance);
    history = tcObject.getHistory();
    assertEquals(10, history.size());

    call = (MockTCObject.MethodCall) history.get(0);
    assertEquals(SerializationUtil.ADD, call.method);
    assertEquals(new Integer(1), call.parameters[0]);

    call = (MockTCObject.MethodCall) history.get(1);
    // Vector implementation calls insertElementAt() internally, hopefully this is constant over all of time
    assertEquals(SerializationUtil.INSERT_AT, call.method);
    assertEquals(new Integer(0), call.parameters[0]);
    assertEquals(new Integer(1), call.parameters[1]);

    call = (MockTCObject.MethodCall) history.get(2);
    assertEquals(SerializationUtil.ADD_AT, call.method);
    assertEquals(new Integer(0), call.parameters[0]);
    assertEquals("Hello", call.parameters[1]);

    call = (MockTCObject.MethodCall) history.get(3);
    assertEquals(SerializationUtil.ADD_AT, call.method);
    assertEquals(new Integer(1), call.parameters[0]);
    assertEquals("world", call.parameters[1]);

    call = (MockTCObject.MethodCall) history.get(4);
    if (Vm.isIBM()) {
      // the IBM JDK delegates all add() method calls internally to addAt()
      assertEquals(SerializationUtil.ADD_AT, call.method);
      assertEquals(new Integer(4), call.parameters[0]);
      assertEquals("Hello", call.parameters[1]);
    } else {
      assertEquals(SerializationUtil.ADD, call.method);
      assertEquals("Hello", call.parameters[0]);
    }

    call = (MockTCObject.MethodCall) history.get(5);
    if (Vm.isIBM()) {
      // the IBM JDK delegates all add() method calls internally to addAt()
      assertEquals(SerializationUtil.ADD_AT, call.method);
      assertEquals(new Integer(5), call.parameters[0]);
      assertEquals("world", call.parameters[1]);
    } else {
      assertEquals(SerializationUtil.ADD, call.method);
      assertEquals("world", call.parameters[0]);
    }

    call = (MockTCObject.MethodCall) history.get(6);
    assertEquals(SerializationUtil.REMOVE_AT, call.method);
    assertEquals(new Integer(1), call.parameters[0]);

    call = (MockTCObject.MethodCall) history.get(7);
    assertEquals(SerializationUtil.SET, call.method);
    assertEquals(new Integer(1), call.parameters[0]);
    assertEquals(new Integer(2), call.parameters[1]);

    call = (MockTCObject.MethodCall) history.get(8);
    assertEquals(SerializationUtil.SET_ELEMENT, call.method);
    // These are supposed to be reversed
    assertEquals(new Integer(1), call.parameters[0]);
    assertEquals(new Integer(69), call.parameters[1]);

    call = (MockTCObject.MethodCall) history.get(9);
    assertEquals(SerializationUtil.CLEAR, call.method);
    params = call.parameters;
    assertEquals(0, params.length);
  }

  public void testStack() throws Exception {
    clazz = Stack.class;
    instance = clazz.newInstance();
    objManager.lookupOrCreate(instance);

    invokeMethod(clazz, instance, SerializationUtil.PUSH_SIGNATURE, new Class[] { Object.class },
                 new Object[] { new Integer(1) });
    invokeMethod(clazz, instance, SerializationUtil.POP_SIGNATURE, new Class[] {}, new Object[] {});

    tcObject = (MockTCObject) objManager.lookupOrCreate(instance);
    history = tcObject.getHistory();
    assertEquals(2, history.size());

    call = (MockTCObject.MethodCall) history.get(0);
    assertEquals(SerializationUtil.ADD, call.method);
    assertEquals(new Integer(1), call.parameters[0]);

    call = (MockTCObject.MethodCall) history.get(1);
    assertEquals(SerializationUtil.REMOVE_AT, call.method);
    assertEquals(new Integer(0), call.parameters[0]);
  }

  public void testArrayList() throws Exception {
    // ArrayList
    clazz = ArrayList.class;
    instance = clazz.newInstance();
    objManager.lookupOrCreate(instance);
    invokeMethod(clazz, instance, SerializationUtil.ADD_SIGNATURE, new Class[] { Object.class },
                 new Object[] { new Integer(1) });
    invokeMethod(clazz, instance, SerializationUtil.ADD_AT_SIGNATURE, new Class[] { int.class, Object.class },
                 new Object[] { new Integer(0), new Integer(1) });
    LinkedList l = new LinkedList();
    l.add("Hello");
    l.add("world");
    invokeMethod(clazz, instance, SerializationUtil.ADD_ALL_AT_SIGNATURE, new Class[] { int.class, Collection.class },
                 new Object[] { new Integer(0), l });
    invokeMethod(clazz, instance, SerializationUtil.ADD_ALL_SIGNATURE, new Class[] { Collection.class },
                 new Object[] { l });
    invokeMethod(clazz, instance, SerializationUtil.REMOVE_AT_SIGNATURE, new Class[] { int.class },
                 new Object[] { new Integer(1) });
    invokeMethod(clazz, instance, SerializationUtil.SET_SIGNATURE, new Class[] { int.class, Object.class },
                 new Object[] { new Integer(1), new Integer(2) });
    invokeMethod(clazz, instance, SerializationUtil.CLEAR_SIGNATURE, new Class[] {}, new Object[] {});

    tcObject = (MockTCObject) objManager.lookupOrCreate(instance);
    history = tcObject.getHistory();
    assertEquals(9, history.size());

    call = (MockTCObject.MethodCall) history.get(0);
    assertEquals(SerializationUtil.ADD, call.method);
    assertEquals(new Integer(1), call.parameters[0]);

    call = (MockTCObject.MethodCall) history.get(1);
    assertEquals(SerializationUtil.ADD_AT, call.method);
    assertEquals(new Integer(0), call.parameters[0]);
    assertEquals(new Integer(1), call.parameters[1]);

    call = (MockTCObject.MethodCall) history.get(2);
    assertEquals(SerializationUtil.ADD_AT, call.method);
    assertEquals(new Integer(0), call.parameters[0]);
    assertEquals("Hello", call.parameters[1]);

    call = (MockTCObject.MethodCall) history.get(3);
    assertEquals(SerializationUtil.ADD_AT, call.method);
    assertEquals(new Integer(1), call.parameters[0]);
    assertEquals("world", call.parameters[1]);

    call = (MockTCObject.MethodCall) history.get(4);
    assertEquals(SerializationUtil.ADD, call.method);
    assertEquals("Hello", call.parameters[0]);

    call = (MockTCObject.MethodCall) history.get(5);
    assertEquals(SerializationUtil.ADD, call.method);
    assertEquals("world", call.parameters[0]);

    call = (MockTCObject.MethodCall) history.get(6);
    assertEquals(SerializationUtil.REMOVE_AT, call.method);
    assertEquals(new Integer(1), call.parameters[0]);

    call = (MockTCObject.MethodCall) history.get(7);
    assertEquals(SerializationUtil.SET, call.method);
    assertEquals(new Integer(1), call.parameters[0]);
    assertEquals(new Integer(2), call.parameters[1]);

    call = (MockTCObject.MethodCall) history.get(8);
    assertEquals(SerializationUtil.CLEAR, call.method);
    params = call.parameters;
    assertEquals(0, params.length);
  }

  public void testLinkedList() throws Exception {
    // LinkedList
    clazz = LinkedList.class;
    instance = clazz.newInstance();
    objManager.lookupOrCreate(instance);
    invokeMethod(clazz, instance, SerializationUtil.ADD_SIGNATURE, new Class[] { Object.class },
                 new Object[] { new Integer(1) });
    invokeMethod(clazz, instance, SerializationUtil.ADD_AT_SIGNATURE, new Class[] { int.class, Object.class },
                 new Object[] { new Integer(0), new Integer(1) });
    LinkedList l = new LinkedList();
    l.add("Hello");
    l.add("world");
    invokeMethod(clazz, instance, SerializationUtil.ADD_ALL_AT_SIGNATURE, new Class[] { int.class, Collection.class },
                 new Object[] { new Integer(0), l });

    invokeMethod(clazz, instance, SerializationUtil.ADD_ALL_SIGNATURE, new Class[] { Collection.class },
                 new Object[] { l });
    invokeMethod(clazz, instance, SerializationUtil.ADD_FIRST_SIGNATURE, new Class[] { Object.class },
                 new Object[] { new Integer(2) });
    invokeMethod(clazz, instance, SerializationUtil.ADD_LAST_SIGNATURE, new Class[] { Object.class },
                 new Object[] { new Integer(3) });
    invokeMethod(clazz, instance, SerializationUtil.REMOVE_AT_SIGNATURE, new Class[] { int.class },
                 new Object[] { new Integer(1) });
    invokeMethod(clazz, instance, SerializationUtil.REMOVE_SIGNATURE, new Class[] { Object.class },
                 new Object[] { "Hello" });
    invokeMethod(clazz, instance, SerializationUtil.SET_SIGNATURE, new Class[] { int.class, Object.class },
                 new Object[] { new Integer(1), new Integer(2) });
    invokeMethod(clazz, instance, SerializationUtil.CLEAR_SIGNATURE, new Class[] {}, new Object[] {});

    tcObject = (MockTCObject) objManager.lookupOrCreate(instance);
    history = tcObject.getHistory();
    assertEquals(12, history.size());

    call = (MockTCObject.MethodCall) history.get(0);
    assertEquals(SerializationUtil.ADD, call.method);
    assertEquals(new Integer(1), call.parameters[0]);

    call = (MockTCObject.MethodCall) history.get(1);
    assertEquals(SerializationUtil.ADD_AT, call.method);
    assertEquals(new Integer(0), call.parameters[0]);
    assertEquals(new Integer(1), call.parameters[1]);

    call = (MockTCObject.MethodCall) history.get(2);
    assertEquals(SerializationUtil.ADD_AT, call.method);
    assertEquals(new Integer(0), call.parameters[0]);
    assertEquals("Hello", call.parameters[1]);

    call = (MockTCObject.MethodCall) history.get(3);
    assertEquals(SerializationUtil.ADD_AT, call.method);
    assertEquals(new Integer(1), call.parameters[0]);
    assertEquals("world", call.parameters[1]);

    call = (MockTCObject.MethodCall) history.get(4);
    assertEquals(SerializationUtil.ADD_AT, call.method);
    assertEquals("Hello", call.parameters[1]);

    call = (MockTCObject.MethodCall) history.get(5);
    assertEquals(SerializationUtil.ADD_AT, call.method);
    assertEquals("world", call.parameters[1]);

    call = (MockTCObject.MethodCall) history.get(6);
    assertEquals(SerializationUtil.ADD_FIRST, call.method);
    assertEquals(new Integer(2), call.parameters[0]);

    call = (MockTCObject.MethodCall) history.get(7);
    assertEquals(SerializationUtil.ADD_LAST, call.method);
    assertEquals(new Integer(3), call.parameters[0]);

    call = (MockTCObject.MethodCall) history.get(8);
    assertEquals(SerializationUtil.REMOVE_AT, call.method);
    assertEquals(new Integer(1), call.parameters[0]);

    call = (MockTCObject.MethodCall) history.get(9);
    assertEquals(SerializationUtil.REMOVE, call.method);
    assertEquals("Hello", call.parameters[0]);

    call = (MockTCObject.MethodCall) history.get(10);
    assertEquals(SerializationUtil.SET, call.method);
    assertEquals(new Integer(1), call.parameters[0]);
    assertEquals(new Integer(2), call.parameters[1]);

    call = (MockTCObject.MethodCall) history.get(11);
    assertEquals(SerializationUtil.CLEAR, call.method);
    params = call.parameters;
    assertEquals(0, params.length);

  }

  public void testHashSet() throws Exception {
    clazz = HashSet.class;
    instance = clazz.newInstance();
    objManager.lookupOrCreate(instance);
    int callCount = 0;
    int checkCount = 0;
    invokeMethod(clazz, instance, SerializationUtil.ADD_SIGNATURE, new Class[] { Object.class },
                 new Object[] { new Integer(1) });
    callCount++;

    invokeMethod(clazz, instance, SerializationUtil.REMOVE_SIGNATURE, new Class[] { Object.class },
                 new Object[] { new Integer(1) });

    callCount++;

    invokeMethod(clazz, instance, SerializationUtil.CLEAR_SIGNATURE, new Class[] {}, new Object[] {});
    callCount++;

    tcObject = (MockTCObject) objManager.lookupOrCreate(instance);
    history = tcObject.getHistory();

    assertEquals(callCount, history.size());

    call = (MockTCObject.MethodCall) history.get(checkCount++);
    assertEquals(SerializationUtil.ADD, call.method);
    assertEquals(new Integer(1), call.parameters[0]);

    call = (MockTCObject.MethodCall) history.get(checkCount++);
    assertEquals(SerializationUtil.REMOVE, call.method);
    assertEquals(new Integer(1), call.parameters[0]);

    call = (MockTCObject.MethodCall) history.get(checkCount++);
    assertEquals(SerializationUtil.CLEAR, call.method);
    assertEquals(0, call.parameters.length);

  }

}