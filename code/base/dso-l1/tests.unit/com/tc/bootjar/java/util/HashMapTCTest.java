/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.bootjar.java.util;

import org.apache.xmlbeans.XmlObject;

import com.tc.config.schema.dynamic.ConfigItem;
import com.tc.exception.ImplementMe;
import com.tc.object.MockRemoteSearchRequestManager;
import com.tc.object.MockTCObject;
import com.tc.object.ObjectID;
import com.tc.object.PortabilityImpl;
import com.tc.object.SerializationUtil;
import com.tc.object.TestClientObjectManager;
import com.tc.object.MockTCObject.MethodCall;
import com.tc.object.bytecode.Manageable;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.schema.DSORuntimeLoggingOptions;
import com.tc.object.config.schema.DSORuntimeOutputOptions;
import com.tc.object.loaders.IsolationClassLoader;
import com.tc.object.locks.MockClientLockManager;
import com.tc.object.tx.MockTransactionManager;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;
import com.tc.util.runtime.Vm;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.security.SecureRandom;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;

public class HashMapTCTest extends TCTestCase {
  private ClassLoader origThreadContextClassLoader;

  @Override
  protected void setUp() throws Exception {
    ClassLoader loader = getClass().getClassLoader();
    InvocationHandler handler = new InvocationHandler() {
      public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String name = method.getName();
        if ("getNewCommonL1Config".equals(name) || "getInstrumentationLoggingOptions".equals(name)
            || "instrumentationLoggingOptions".equals(name) || "getLogicalExtendingClassName".equals(name)
            || "createDsoClassAdapterFor".equals(name) || "getModulesForInitialization".equals(name)
            || "verifyBootJarContents".equals(name) || "validateSessionConfig".equals(name) || "getUUID".equals(name)) {
          return null;
        } else if ("shouldBeAdapted".equals(name)) {
          return Boolean.FALSE;
        } else if ("isNeverAdaptable".equals(name)) {
          return Boolean.TRUE;
        } else if ("isLogical".equals(name)) {
          return Boolean.TRUE;
        } else if ("hasBootJar".equals(name)) {
          return Boolean.TRUE;
        } else if ("getAppGroup".equals(name)) {
          return null;
        } else if ("getAspectModules".equals(name)) {
          return new HashMap();
        } else if ("getPortability".equals(name)) {
          return new PortabilityImpl((DSOClientConfigHelper) proxy);
        } else if ("runtimeLoggingOptions".equals(name)) {
          return new MockRuntimeOptions();
        } else if ("runtimeOutputOptions".equals(name)) {
          return new MockOutputOptions();
        } else if ("getClassResource".equals(name)) {
          return null;
        } else if (Vm.isIBM() && "isRoot".equals(name)
                   && ("java.lang.reflect.Method".equals(args[0]) || "java.lang.reflect.Constructor".equals(args[0]))) {
          // the implementation of java.lang.Class in the IBM JDK is different and caches
          // fields of the Method and Constructor classes, which it retrieves afterwards by
          // calling the Field.get method. This gets into the AccessibleObject changes for
          // DSO, which checks if the returned value is a root
          return Boolean.FALSE;
        }

        throw new ImplementMe();
      }
    };
    Object proxy = Proxy.newProxyInstance(loader, new Class[] { DSOClientConfigHelper.class }, handler);

    TestClientObjectManager testClientObjectManager = new TestClientObjectManager();
    MockTransactionManager testTransactionManager = new MockTransactionManager();
    MockClientLockManager testClientLockManager = new MockClientLockManager();
    MockRemoteSearchRequestManager testSearchRequestManager = new MockRemoteSearchRequestManager();
    IsolationClassLoader classLoader = new IsolationClassLoader((DSOClientConfigHelper) proxy, testClientObjectManager,
                                                                testTransactionManager, testClientLockManager, testSearchRequestManager);
    classLoader.init();

    this.origThreadContextClassLoader = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(classLoader);
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();

    Thread.currentThread().setContextClassLoader(this.origThreadContextClassLoader);
  }

  public void testLogicalInvoke() throws Exception {
    Map tcmap = createMap("java.util.HashMap");
    validateLogicalInvoke(tcmap);

    tcmap = createMap("java.util.LinkedHashMap");
    validateLogicalInvoke(tcmap);

    tcmap = createMap("java.util.Hashtable");
    validateLogicalInvoke(tcmap);
  }

  public void BROKENtestMapNotShared() throws Exception {
    Map jmap = createMap("java.util.HashMap_J");
    Map tcmap = createMap("java.util.HashMap");
    validateMapNotShared(jmap, tcmap);

    jmap = createMap("java.util.LinkedHashMap_J");
    tcmap = createMap("java.util.LinkedHashMap");
    validateMapNotShared(jmap, tcmap);

    jmap = createMap("java.util.Hashtable_J");
    tcmap = createMap("java.util.Hashtable");
    validateMapNotShared(jmap, tcmap);
  }

  public void BROKENtestEntrySet() throws Exception {
    Map jmap = createMap("java.util.HashMap_J");
    Map tcmap = createMap("java.util.HashMap");
    validateEntrySet(jmap, tcmap);

    jmap = createMap("java.util.LinkedHashMap_J");
    tcmap = createMap("java.util.LinkedHashMap");
    validateEntrySet(jmap, tcmap);

    jmap = createMap("java.util.Hashtable_J");
    tcmap = createMap("java.util.Hashtable");
    validateEntrySet(jmap, tcmap);
  }

  public void BROKENtestEntrySetAdd() throws Exception {
    // no test for entrySet().add as it throws an UnsupportedOperationException by design.
  }

  public void BROKENtestEntrySetRemove() throws Exception {
    Map jmap = createMap("java.util.HashMap_J");
    Map tcmap = createMap("java.util.HashMap");
    validateEntrySetRemove(jmap, tcmap);

    jmap = createMap("java.util.LinkedHashMap_J");
    tcmap = createMap("java.util.LinkedHashMap");
    validateEntrySetRemove(jmap, tcmap);

    jmap = createMap("java.util.Hashtable_J");
    tcmap = createMap("java.util.Hashtable");
    validateEntrySetRemove(jmap, tcmap);
  }

  public void BROKENtestEntrySetClear() throws Exception {
    Map jmap = createMap("java.util.HashMap_J");
    Map tcmap = createMap("java.util.HashMap");
    validateEntrySetClear(jmap, tcmap);

    jmap = createMap("java.util.LinkedHashMap_J");
    tcmap = createMap("java.util.LinkedHashMap");
    validateEntrySetClear(jmap, tcmap);

    jmap = createMap("java.util.Hashtable_J");
    tcmap = createMap("java.util.Hashtable");
    validateEntrySetClear(jmap, tcmap);
  }

  public void validateEntrySetIteratorRemove() throws Exception {
    Map jmap = createMap("java.util.HashMap_J");
    Map tcmap = createMap("java.util.HashMap");
    validateEntryIteratorRemove(jmap, tcmap);

    jmap = createMap("java.util.LinkedHashMap_J");
    tcmap = createMap("java.util.LinkedHashMap");
    validateEntryIteratorRemove(jmap, tcmap);

    jmap = createMap("java.util.Hashtable_J");
    tcmap = createMap("java.util.Hashtable");
    validateEntryIteratorRemove(jmap, tcmap);
  }

  public void BROKENtestKeySet() throws Exception {
    Map jmap = createMap("java.util.HashMap_J");
    Map tcmap = createMap("java.util.HashMap");
    validateKeySet(jmap, tcmap);

    jmap = createMap("java.util.LinkedHashMap_J");
    tcmap = createMap("java.util.LinkedHashMap");
    validateKeySet(jmap, tcmap);

    jmap = createMap("java.util.Hashtable_J");
    tcmap = createMap("java.util.Hashtable");
    validateKeySet(jmap, tcmap);
  }

  public void BROKENtestKeySetRemove() throws Exception {
    Map jmap = createMap("java.util.HashMap_J");
    Map tcmap = createMap("java.util.HashMap");
    validateKeySetRemove(jmap, tcmap);

    jmap = createMap("java.util.LinkedHashMap_J");
    tcmap = createMap("java.util.LinkedHashMap");
    validateKeySetRemove(jmap, tcmap);

    jmap = createMap("java.util.Hashtable_J");
    tcmap = createMap("java.util.Hashtable");
    validateKeySetRemove(jmap, tcmap);
  }

  public void BROKENtestKeySetClear() throws Exception {
    Map jmap = createMap("java.util.HashMap_J");
    Map tcmap = createMap("java.util.HashMap");
    validateKeySetClear(jmap, tcmap);

    jmap = createMap("java.util.LinkedHashMap_J");
    tcmap = createMap("java.util.LinkedHashMap");
    validateKeySetClear(jmap, tcmap);

    jmap = createMap("java.util.Hashtable_J");
    tcmap = createMap("java.util.Hashtable");
    validateKeySetClear(jmap, tcmap);
  }

  public void BROKENtestKeySetIteratorRemove() throws Exception {
    Map jmap = createMap("java.util.HashMap_J");
    Map tcmap = createMap("java.util.HashMap");
    validateKeySetIteratorRemove(jmap, tcmap);

    jmap = createMap("java.util.LinkedHashMap_J");
    tcmap = createMap("java.util.LinkedHashMap");
    validateKeySetIteratorRemove(jmap, tcmap);

    jmap = createMap("java.util.Hashtable_J");
    tcmap = createMap("java.util.Hashtable");
    validateKeySetIteratorRemove(jmap, tcmap);
  }

  public void BROKENtestValues() throws Exception {
    Map jmap = createMap("java.util.HashMap_J");
    Map tcmap = createMap("java.util.HashMap");
    validateValues(jmap, tcmap);

    jmap = createMap("java.util.LinkedHashMap_J");
    tcmap = createMap("java.util.LinkedHashMap");
    validateValues(jmap, tcmap);

    jmap = createMap("java.util.Hashtable_J");
    tcmap = createMap("java.util.Hashtable");
    validateValues(jmap, tcmap);
  }

  public void BROKENtestValuesRemove() throws Exception {
    Map jmap = createMap("java.util.HashMap_J");
    Map tcmap = createMap("java.util.HashMap");
    validateValuesRemove(jmap, tcmap);

    jmap = createMap("java.util.LinkedHashMap_J");
    tcmap = createMap("java.util.LinkedHashMap");
    validateValuesRemove(jmap, tcmap);

    jmap = createMap("java.util.Hashtable_J");
    tcmap = createMap("java.util.Hashtable");
    validateValuesRemove(jmap, tcmap);
  }

  public void BROKENtestValuesClear() throws Exception {
    Map jmap = createMap("java.util.HashMap_J");
    Map tcmap = createMap("java.util.HashMap");
    validateValuesClear(jmap, tcmap);

    jmap = createMap("java.util.LinkedHashMap_J");
    tcmap = createMap("java.util.LinkedHashMap");
    validateValuesClear(jmap, tcmap);

    jmap = createMap("java.util.Hashtable_J");
    tcmap = createMap("java.util.Hashtable");
    validateValuesClear(jmap, tcmap);
  }

  public void BROKENtestValuesIteratorRemove() throws Exception {
    Map jmap = createMap("java.util.HashMap_J");
    Map tcmap = createMap("java.util.HashMap");
    validateValuesIteratorRemove(jmap, tcmap);

    jmap = createMap("java.util.LinkedHashMap_J");
    tcmap = createMap("java.util.LinkedHashMap");
    validateValuesIteratorRemove(jmap, tcmap);

    jmap = createMap("java.util.Hashtable_J");
    tcmap = createMap("java.util.Hashtable");
    validateValuesIteratorRemove(jmap, tcmap);
  }

  /** ****************************************************************************** */

  public void validateLogicalInvoke(Map tcmap) throws Exception {

    MockTCObject tcObject = new MockTCObject(new ObjectID(1), this, false, true);

    ((Manageable) tcmap).__tc_managed(tcObject);
    SecureRandom sr = new SecureRandom();
    long seed = sr.nextLong();
    populateMap(tcmap, new Random(seed), 100);

    List logicalInvokeHistory = tcObject.getHistory();

    assertEqualsContent(logicalInvokeHistory, tcmap);
  }

  public void validateMapNotShared(Map jmap, Map tcmap) throws Exception {
    SecureRandom sr = new SecureRandom();
    long seed = sr.nextLong();
    System.err.println("Seed is " + seed);
    populateMap(jmap, new Random(seed));
    populateMap(tcmap, new Random(seed));

    assertEquals(jmap, tcmap);

    System.err.println("Size is " + tcmap.size());
    assertEquals(tcmap, jmap);
  }

  public void validateEntrySet(Map jmap, Map tcmap) throws Exception {
    SecureRandom sr = new SecureRandom();
    long seed = sr.nextLong();
    populateMap(jmap, new Random(seed));
    populateMap(tcmap, new Random(seed));

    assertEqualsCollection(jmap.entrySet(), tcmap.entrySet());
  }

  public void validateEntrySetRemove(Map jmap, Map tcmap) throws Exception {
    populateMapWithHashKey(jmap, 100);
    populateMapWithHashKey(tcmap, 100);

    Map.Entry entryToBeRemove = new SimpleEntry(new HashKey(10), new HashValue(10));

    Set tcentrySet = tcmap.entrySet();
    tcentrySet.remove(entryToBeRemove);

    Set jentrySet = jmap.entrySet();
    jentrySet.remove(entryToBeRemove);

    assertEquals(99, jmap.size());
    assertEquals(99, tcmap.size());
    assertEquals(jmap, tcmap);
    assertEquals(tcmap, jmap);
  }

  public void validateEntrySetClear(Map jmap, Map tcmap) throws Exception {
    populateMapWithHashKey(jmap, 100);
    populateMapWithHashKey(tcmap, 100);

    Set tcentrySet = tcmap.entrySet();
    tcentrySet.clear();

    Set jentrySet = jmap.entrySet();
    jentrySet.clear();

    assertEquals(0, jmap.size());
    assertEquals(0, tcmap.size());
  }

  public void validateEntryIteratorRemove(Map jmap, Map tcmap) throws Exception {
    populateMapWithHashKey(jmap, 100);
    populateMapWithHashKey(tcmap, 100);

    Iterator tcIterator = tcmap.entrySet().iterator();
    tcIterator.next();
    tcIterator.next();
    tcIterator.remove();

    Iterator jIterator = jmap.entrySet().iterator();
    jIterator.next();
    jIterator.next();
    jIterator.remove();

    assertEquals(99, jmap.size());
    assertEquals(99, tcmap.size());
    assertEquals(jmap, tcmap);
    assertEquals(tcmap, jmap);
  }

  public void validateKeySet(Map jmap, Map tcmap) throws Exception {
    SecureRandom sr = new SecureRandom();
    long seed = sr.nextLong();
    populateMap(jmap, new Random(seed));
    populateMap(tcmap, new Random(seed));

    assertEqualsCollection(jmap.keySet(), tcmap.keySet());
  }

  public void validateKeySetRemove(Map jmap, Map tcmap) throws Exception {
    populateMapWithHashKey(jmap, 100);
    populateMapWithHashKey(tcmap, 100);

    HashKey keyToBeRemoved = new HashKey(10);

    Set tckeySet = tcmap.keySet();
    tckeySet.remove(keyToBeRemoved);

    Set jkeySet = jmap.keySet();
    jkeySet.remove(keyToBeRemoved);

    assertEquals(99, jmap.size());
    assertEquals(99, tcmap.size());
    assertEquals(jmap, tcmap);
    assertEquals(tcmap, jmap);
  }

  public void validateKeySetClear(Map jmap, Map tcmap) throws Exception {
    populateMapWithHashKey(jmap, 100);
    populateMapWithHashKey(tcmap, 100);

    Set tckeySet = tcmap.keySet();
    tckeySet.clear();

    Set jkeySet = jmap.keySet();
    jkeySet.clear();

    assertEquals(0, jmap.size());
    assertEquals(0, tcmap.size());
  }

  public void validateKeySetIteratorRemove(Map jmap, Map tcmap) throws Exception {
    populateMapWithHashKey(jmap, 100);
    populateMapWithHashKey(tcmap, 100);

    Iterator tciterator = tcmap.keySet().iterator();
    tciterator.next();
    tciterator.next();
    tciterator.remove();

    Iterator jiterator = jmap.keySet().iterator();
    jiterator.next();
    jiterator.next();
    jiterator.remove();

    assertEquals(99, jmap.size());
    assertEquals(99, tcmap.size());
    assertEquals(jmap, tcmap);
    assertEquals(tcmap, jmap);
  }

  public void validateValues(Map jmap, Map tcmap) throws Exception {
    SecureRandom sr = new SecureRandom();
    long seed = sr.nextLong();
    populateMap(jmap, new Random(seed));
    populateMap(tcmap, new Random(seed));

    assertEqualsCollection(jmap.values(), tcmap.values());
  }

  public void validateValuesRemove(Map jmap, Map tcmap) throws Exception {
    populateMapWithHashKey(jmap, 100);
    populateMapWithHashKey(tcmap, 100);

    HashValue valueToBeRemoved = new HashValue(10);

    Collection tcvalues = tcmap.values();
    tcvalues.remove(valueToBeRemoved);

    Collection jvalues = jmap.values();
    jvalues.remove(valueToBeRemoved);

    assertEquals(99, jmap.size());
    assertEquals(99, tcmap.size());
    assertEquals(jmap, tcmap);
    assertEquals(tcmap, jmap);
  }

  public void validateValuesClear(Map jmap, Map tcmap) throws Exception {
    populateMapWithHashKey(jmap, 100);
    populateMapWithHashKey(tcmap, 100);

    Collection tcvalues = tcmap.values();
    tcvalues.clear();

    Collection jvalues = jmap.values();
    jvalues.clear();

    assertEquals(0, jmap.size());
    assertEquals(0, tcmap.size());
  }

  public void validateValuesIteratorRemove(Map jmap, Map tcmap) throws Exception {
    populateMapWithHashKey(jmap, 100);
    populateMapWithHashKey(tcmap, 100);

    Iterator tciterator = tcmap.values().iterator();
    tciterator.next();
    tciterator.next();
    tciterator.remove();

    Iterator jiterator = jmap.values().iterator();
    jiterator.next();
    jiterator.next();
    jiterator.remove();

    assertEquals(99, jmap.size());
    assertEquals(99, tcmap.size());
    assertEquals(jmap, tcmap);
    assertEquals(tcmap, jmap);
  }

  void assertSingleMapping(Map map, final Object key, final Object value) {
    Assert.assertFalse(map.isEmpty());
    Assert.assertEquals(1, map.size());
    Assert.assertEquals(1, map.entrySet().size());
    Assert.assertEquals(1, map.values().size());
    Assert.assertEquals(1, map.keySet().size());
    Assert.assertEquals(value, map.get(key));
    Assert.assertTrue(map.containsKey(key));
    Assert.assertTrue(map.containsValue(value));

    Set entries = map.entrySet();
    for (Iterator i = entries.iterator(); i.hasNext();) {
      Entry entry = (Entry) i.next();
      Assert.assertEquals(key, entry.getKey());
      Assert.assertEquals(value, entry.getValue());
    }

    for (Iterator i = map.values().iterator(); i.hasNext();) {
      Object o = i.next();
      Assert.assertEquals(value, o);
    }

    for (Iterator i = map.keySet().iterator(); i.hasNext();) {
      Object o = i.next();
      Assert.assertEquals(key, o);
    }

    HashMap compare = new HashMap();
    compare.put(key, value);
    Assert.assertEquals(compare, map);
  }

  private void assertEqualsContent(Collection expected, Map actual) {
    assertEquals(expected.size(), actual.size());

    Set keySet = actual.keySet();
    for (Iterator i = expected.iterator(); i.hasNext();) {
      MethodCall methodCall = (MethodCall) i.next();
      assertEquals(SerializationUtil.PUT, methodCall.method);

      assertTrue(keySet.contains(methodCall.parameters[0]));
      assertEquals(methodCall.parameters[1], actual.get(methodCall.parameters[0]));
    }
  }

  private void assertEqualsCollection(Collection expected, Collection actual) {
    assertEquals(expected.size(), actual.size());

    for (Iterator i = expected.iterator(), j = actual.iterator(); i.hasNext();) {
      Object expectedObject = i.next();
      Object actualObject = j.next();
      assertEquals(expectedObject, actualObject);
    }
  }

  private Map createMap(String className) throws ClassNotFoundException, SecurityException, NoSuchMethodException,
      IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
    Class c = Class.forName(className);
    Constructor constructor = c.getConstructor(new Class[0]);
    return (Map) constructor.newInstance(new Object[0]);
  }

  private void populateMapWithHashKey(Map map, int numOfItems) {
    for (int i = 0; i < numOfItems; i++) {
      map.put(new HashKey(i), new HashValue(i));
    }
  }

  private void populateMap(Map map, Random r) {
    populateMap(map, r, 10000);
  }

  private void populateMap(Map map, Random r, int numOfItems) {
    for (int i = 0; i < numOfItems; i++) {
      switch (i % 4) {
        case 0:
          map.put(new Long(r.nextLong()), "Long to String");
          break;
        case 1:
          map.put(new Integer(r.nextInt()), "Integer to String");
          break;
        case 2:
          map.put(String.valueOf(r.nextLong()), "String to String");
          break;
        case 3:
          map.put(new ObjectID(r.nextLong()), "ObjectID to String");
          break;
        default:
          // Should never come here
          throw new AssertionError();
      }
    }
  }

  private static class SimpleEntry implements Map.Entry {

    private final Object key;
    private Object       value;

    public SimpleEntry(Object key, Object value) {
      this.key = key;
      this.value = value;
    }

    public Object getKey() {
      return key;
    }

    public Object getValue() {
      return value;
    }

    public Object setValue(Object value) {
      Object oldValue = this.value;
      this.value = value;
      return oldValue;
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof Map.Entry)) return false;
      Map.Entry e = (Map.Entry) o;
      return eq(key, e.getKey()) && eq(value, e.getValue());
    }

    @Override
    public int hashCode() {
      return ((key == null) ? 0 : key.hashCode()) ^ ((value == null) ? 0 : value.hashCode());
    }

    @Override
    public String toString() {
      return key + "=" + value;
    }

    private static boolean eq(Object o1, Object o2) {
      return (o1 == null ? o2 == null : o1.equals(o2));
    }
  }

  private static class HashKey {
    private final int i;

    public HashKey(int i) {
      super();
      this.i = i;
    }

    @Override
    public int hashCode() {
      return i;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == null) return false;
      if (!(obj instanceof HashKey)) return false;
      return ((HashKey) obj).i == i;
    }
  }

  private static class HashValue {
    private final int i;

    public HashValue(int i) {
      super();
      this.i = i;
    }

    @Override
    public int hashCode() {
      return i;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == null) return false;
      if (!(obj instanceof HashValue)) return false;
      return ((HashValue) obj).i == i;
    }

    @Override
    public String toString() {
      return super.toString() + ", i: " + i;
    }
  }

  private static class MockRuntimeOptions implements DSORuntimeLoggingOptions {

    public boolean logDistributedMethodDebug() {
      return false;
    }

    public boolean logFieldChangeDebug() {
      return false;
    }

    public boolean logLockDebug() {
      return false;
    }

    public boolean logNamedLoaderDebug() {
      return false;
    }

    public boolean logNewObjectDebug() {
      return false;
    }

    public boolean logNonPortableDump() {
      return false;
    }

    public boolean logWaitNotifyDebug() {
      return false;
    }

    public void changesInItemForbidden(ConfigItem item) {
      //
    }

    public void changesInItemIgnored(ConfigItem item) {
      //
    }

    public XmlObject getBean() {
      return null;
    }

  }

  private static class MockOutputOptions implements DSORuntimeOutputOptions {

    public boolean doAutoLockDetails() {
      return false;
    }

    public boolean doCaller() {
      return false;
    }

    public boolean doFullStack() {
      return false;
    }

    public void changesInItemForbidden(ConfigItem item) {
      //
    }

    public void changesInItemIgnored(ConfigItem item) {
      //
    }

    public XmlObject getBean() {
      return null;
    }

  }

}
