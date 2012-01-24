/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.object.TCObject;
import com.tc.object.bytecode.ClassAdapterBase;
import com.tc.object.bytecode.TransparentAccess;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.builtin.HashMap;
import com.tctest.builtin.HashSet;
import com.tctest.runner.AbstractTransparentApp;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class PhysicalInstrumentedMethodsTestApp extends AbstractTransparentApp {

  private final CyclicBarrier barrier;

  private TestSubSubClass     subRoot;

  public PhysicalInstrumentedMethodsTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    barrier = new CyclicBarrier(getParticipantCount());
  }

  public void run() {
    try {
      int index = barrier.barrier();

      validateGenerateTCObjectFieldAndMethod(index);

      if (index == 0) {
        subRoot = new TestSubSubClass();
      }

      barrier.barrier();

      fieldSettingTest(index);
      fieldGettingTest(index);
    } catch (Throwable t) {
      notifyError(t);
    }
  }

  private void validateGenerateTCObjectFieldAndMethod(int index) {
    if (index == 0) {
      validateGeneratedTCObjectField();
      validateGeneratedTCObjectMethod();
    }
  }

  private void validateGeneratedTCObjectField() {
    Field f = null;
    try {
      f = TestParentClass.class.getDeclaredField(ClassAdapterBase.MANAGED_FIELD_NAME);
      Assert.assertNotNull(f);
    } catch (NoSuchFieldException e) {
      throw new AssertionError("TestParentClass.class does not contain field " + ClassAdapterBase.MANAGED_FIELD_NAME);
    }

    try {
      f = TestSubClass.class.getDeclaredField(ClassAdapterBase.MANAGED_FIELD_NAME);
      throw new AssertionError("Should have thrown an NoSuchFieldException.");
    } catch (NoSuchFieldException e) {
      // ignore Exception
    }

    try {
      f = TestSubSubClass.class.getDeclaredField(ClassAdapterBase.MANAGED_FIELD_NAME);
      throw new AssertionError("Should have thrown an NoSuchFieldException.");
    } catch (NoSuchFieldException e) {
      // ignore Exception
    }
  }

  private void validateGeneratedTCObjectMethod() {
    try {
      TestParentClass.class.getDeclaredMethod(ClassAdapterBase.MANAGED_METHOD, new Class[] { TCObject.class });
    } catch (NoSuchMethodException e) {
      throw new AssertionError("TestParentClass.class does not contain method " + ClassAdapterBase.MANAGED_METHOD);
    }

    try {
      TestSubClass.class.getDeclaredMethod(ClassAdapterBase.MANAGED_METHOD, new Class[] { TCObject.class });
      throw new AssertionError("Should have thrown a NoSuchMethodException.");
    } catch (NoSuchMethodException e) {
      // ignore
    }

    try {
      TestSubSubClass.class.getDeclaredMethod(ClassAdapterBase.MANAGED_METHOD, new Class[] { TCObject.class });
      throw new AssertionError("Should have thrown a NoSuchMethodException.");
    } catch (NoSuchMethodException e) {
      // ignore
    }

  }

  private void fieldSettingTest(int index) throws Exception {
    subClassFieldUnManagedSettingUsingReflectionTest(index);
    subClassFieldUnManagedSettingTest(index);
    subClassFieldManagedSettingUsingReflectionTest(index);
    subClassFieldManagedSettingTest(index);
  }

  private void fieldGettingTest(int index) throws Exception {
    subClassAllFieldsUnManagedGettingTest(index);
    subClassAllFieldsUnManagedGettingUsingReflectionTest(index);
    subClassAllFieldsManagedGettingTest(index);
    subClassAllFieldsManagedGettingUsingReflectionTest(index);
  }

  private void clear() throws Exception {
    synchronized (subRoot) {
      subRoot.clear();
    }

    barrier.barrier();
  }

  private void subClassFieldUnManagedSettingUsingReflectionTest(int index) throws Exception {
    clear();

    Set referenceSet = new HashSet();
    referenceSet.add("first element");
    referenceSet.add("second element");

    Map referenceMap = new HashMap();
    referenceMap.put("first key", "first element");
    referenceMap.put("second key", "second element");

    int[] referenceArray = new int[] { 1, 2 };

    synchronized (subRoot) {
      if (index == 0) {
        Class subRootClass = subRoot.getClass();

        // __tc_setfield will not notify the other clients.
        Method m = subRootClass.getDeclaredMethod("__tc_setfield", new Class[] { String.class, Object.class });
        m.invoke(subRoot, new Object[] {
            "com.tctest.PhysicalInstrumentedMethodsTestApp$TestSubSubClass.grandChildIntField", Integer.valueOf(2) });
        m.invoke(subRoot, new Object[] {
            "com.tctest.PhysicalInstrumentedMethodsTestApp$TestSubSubClass.grandChildObjectField", Integer.valueOf(7) });
        m.invoke(subRoot, new Object[] { "com.tctest.PhysicalInstrumentedMethodsTestApp$TestSubClass.childIntField",
            Integer.valueOf(4) });
        m.invoke(subRoot, new Object[] { "com.tctest.PhysicalInstrumentedMethodsTestApp$TestSubClass.childSetField",
            referenceSet });
        m.invoke(subRoot, new Object[] { "com.tctest.PhysicalInstrumentedMethodsTestApp$TestSubClass.childStringField",
            "testString" });
        m.invoke(subRoot, new Object[] {
            "com.tctest.PhysicalInstrumentedMethodsTestApp$TestParentClass.parentIntField", Integer.valueOf(5) });
        m.invoke(subRoot, new Object[] {
            "com.tctest.PhysicalInstrumentedMethodsTestApp$TestParentClass.parentMapField", referenceMap });
        m.invoke(subRoot, new Object[] {
            "com.tctest.PhysicalInstrumentedMethodsTestApp$TestParentClass.parentArrayField", referenceArray });

        // This setting should do nothing.
        m.invoke(subRoot, new Object[] { "com.tctest.PhysicalInstrumentedMethodsTestApp$TestSubSubClass.this$0", null });
        TransparentAccess ta = (TransparentAccess) subRoot;
        Assert.assertNotNull(ta
            .__tc_getmanagedfield("com.tctest.PhysicalInstrumentedMethodsTestApp$TestSubSubClass.this$0"));

        Assert.assertEquals(2, subRoot.getGrandChildIntField());
        Assert.assertEquals(Integer.valueOf(7), subRoot.getGrandChildObjectField());
        Assert.assertEquals(4, subRoot.getChildIntField());
        assertSetsEqual(referenceSet, subRoot.getChildSetField());
        Assert.assertEquals("testString", subRoot.getChildStringField());

        Assert.assertEquals(5, subRoot.getParentIntField());
        assertMappings(referenceMap, subRoot.getParentMapField());
        assertIntegerArray(referenceArray, subRoot.getParentArrayField());

      }
    }
    barrier.barrier();
  }

  private void subClassFieldUnManagedSettingTest(int index) throws Exception {
    clear();

    Set referenceSet = new HashSet();
    referenceSet.add("first element");
    referenceSet.add("second element");

    Map referenceMap = new HashMap();
    referenceMap.put("first key", "first element");
    referenceMap.put("second key", "second element");

    int[] referenceArray = new int[] { 1, 2 };

    synchronized (subRoot) {
      if (index == 0) {
        TransparentAccess ta = (TransparentAccess) subRoot;

        // __tc_setfield will not notify the other clients.
        ta.__tc_setfield("com.tctest.PhysicalInstrumentedMethodsTestApp$TestSubSubClass.grandChildIntField",
                         Integer.valueOf(2));
        ta.__tc_setfield("com.tctest.PhysicalInstrumentedMethodsTestApp$TestSubSubClass.grandChildObjectField",
                         Integer.valueOf(7));
        ta.__tc_setfield("com.tctest.PhysicalInstrumentedMethodsTestApp$TestSubClass.childIntField", Integer.valueOf(4));
        ta.__tc_setfield("com.tctest.PhysicalInstrumentedMethodsTestApp$TestSubClass.childSetField", referenceSet);
        ta.__tc_setfield("com.tctest.PhysicalInstrumentedMethodsTestApp$TestSubClass.childStringField", "testString");
        ta.__tc_setfield("com.tctest.PhysicalInstrumentedMethodsTestApp$TestParentClass.parentIntField",
                         Integer.valueOf(5));
        ta.__tc_setfield("com.tctest.PhysicalInstrumentedMethodsTestApp$TestParentClass.parentMapField", referenceMap);
        ta.__tc_setfield("com.tctest.PhysicalInstrumentedMethodsTestApp$TestParentClass.parentArrayField",
                         referenceArray);

        // This setting should do nothing.
        ta.__tc_setfield("com.tctest.PhysicalInstrumentedMethodsTestApp$TestSubSubClass.this$0", null);

        Assert.assertNotNull(ta
            .__tc_getmanagedfield("com.tctest.PhysicalInstrumentedMethodsTestApp$TestSubSubClass.this$0"));

        Assert.assertEquals(2, subRoot.getGrandChildIntField());
        Assert.assertEquals(Integer.valueOf(7), subRoot.getGrandChildObjectField());
        Assert.assertEquals(4, subRoot.getChildIntField());
        assertSetsEqual(referenceSet, subRoot.getChildSetField());
        Assert.assertEquals("testString", subRoot.getChildStringField());

        Assert.assertEquals(5, subRoot.getParentIntField());
        assertMappings(referenceMap, subRoot.getParentMapField());
        assertIntegerArray(referenceArray, subRoot.getParentArrayField());
      }
    }
    barrier.barrier();
  }

  private void subClassFieldManagedSettingUsingReflectionTest(int index) throws Exception {
    clear();

    Set referenceSet = new HashSet();
    referenceSet.add("first element");
    referenceSet.add("second element");

    Map referenceMap = new HashMap();
    referenceMap.put("first key", "first element");
    referenceMap.put("second key", "second element");

    int[] referenceArray = new int[] { 1, 2 };

    synchronized (subRoot) {
      if (index == 0) {
        Class subRootClass = subRoot.getClass();

        Method m = subRootClass.getDeclaredMethod("__tc_setmanagedfield", new Class[] { String.class, Object.class });
        m.invoke(subRoot, new Object[] {
            "com.tctest.PhysicalInstrumentedMethodsTestApp$TestSubSubClass.grandChildIntField", Integer.valueOf(2) });
        m.invoke(subRoot, new Object[] {
            "com.tctest.PhysicalInstrumentedMethodsTestApp$TestSubSubClass.grandChildObjectField", Integer.valueOf(7) });
        m.invoke(subRoot, new Object[] { "com.tctest.PhysicalInstrumentedMethodsTestApp$TestSubClass.childIntField",
            Integer.valueOf(4) });
        m.invoke(subRoot, new Object[] { "com.tctest.PhysicalInstrumentedMethodsTestApp$TestSubClass.childSetField",
            referenceSet });
        m.invoke(subRoot, new Object[] { "com.tctest.PhysicalInstrumentedMethodsTestApp$TestSubClass.childStringField",
            "testString" });
        m.invoke(subRoot, new Object[] {
            "com.tctest.PhysicalInstrumentedMethodsTestApp$TestParentClass.parentIntField", Integer.valueOf(5) });
        m.invoke(subRoot, new Object[] {
            "com.tctest.PhysicalInstrumentedMethodsTestApp$TestParentClass.parentMapField", referenceMap });
        m.invoke(subRoot, new Object[] {
            "com.tctest.PhysicalInstrumentedMethodsTestApp$TestParentClass.parentArrayField", referenceArray });

        // This setting should do nothing.
        m.invoke(subRoot, new Object[] { "com.tctest.PhysicalInstrumentedMethodsTestApp$TestSubSubClass.this$0", null });
      }
    }

    barrier.barrier();

    TransparentAccess ta = (TransparentAccess) subRoot;
    Assert.assertNotNull(ta
        .__tc_getmanagedfield("com.tctest.PhysicalInstrumentedMethodsTestApp$TestSubSubClass.this$0"));

    Assert.assertEquals(2, subRoot.getGrandChildIntField());
    Assert.assertEquals(Integer.valueOf(7), subRoot.getGrandChildObjectField());
    Assert.assertEquals(4, subRoot.getChildIntField());
    assertSetsEqual(referenceSet, subRoot.getChildSetField());
    Assert.assertEquals("testString", subRoot.getChildStringField());

    Assert.assertEquals(5, subRoot.getParentIntField());
    assertMappings(referenceMap, subRoot.getParentMapField());
    assertIntegerArray(referenceArray, subRoot.getParentArrayField());

    barrier.barrier();
  }

  private void subClassFieldManagedSettingTest(int index) throws Exception {
    clear();

    Set referenceSet = new HashSet();
    referenceSet.add("first element");
    referenceSet.add("second element");

    Map referenceMap = new HashMap();
    referenceMap.put("first key", "first element");
    referenceMap.put("second key", "second element");

    int[] referenceArray = new int[] { 1, 2 };
    TransparentAccess ta = (TransparentAccess) subRoot;

    synchronized (subRoot) {
      if (index == 0) {
        ta.__tc_setmanagedfield("com.tctest.PhysicalInstrumentedMethodsTestApp$TestSubSubClass.grandChildIntField",
                                Integer.valueOf(2));
        ta.__tc_setmanagedfield("com.tctest.PhysicalInstrumentedMethodsTestApp$TestSubSubClass.grandChildObjectField",
                                Integer.valueOf(7));
        ta.__tc_setmanagedfield("com.tctest.PhysicalInstrumentedMethodsTestApp$TestSubClass.childIntField",
                                Integer.valueOf(4));
        ta.__tc_setmanagedfield("com.tctest.PhysicalInstrumentedMethodsTestApp$TestSubClass.childSetField",
                                referenceSet);
        ta.__tc_setmanagedfield("com.tctest.PhysicalInstrumentedMethodsTestApp$TestSubClass.childStringField",
                                "testString");
        ta.__tc_setmanagedfield("com.tctest.PhysicalInstrumentedMethodsTestApp$TestParentClass.parentIntField",
                                Integer.valueOf(5));
        ta.__tc_setmanagedfield("com.tctest.PhysicalInstrumentedMethodsTestApp$TestParentClass.parentMapField",
                                referenceMap);
        ta.__tc_setmanagedfield("com.tctest.PhysicalInstrumentedMethodsTestApp$TestParentClass.parentArrayField",
                                referenceArray);

        // This setting should do nothing.
        ta.__tc_setmanagedfield("com.tctest.PhysicalInstrumentedMethodsTestApp$TestSubSubClass.this$0", null);
      }
    }

    barrier.barrier();

    Assert.assertNotNull(ta
        .__tc_getmanagedfield("com.tctest.PhysicalInstrumentedMethodsTestApp$TestSubSubClass.this$0"));

    Assert.assertEquals(2, subRoot.getGrandChildIntField());
    Assert.assertEquals(Integer.valueOf(7), subRoot.getGrandChildObjectField());
    Assert.assertEquals(4, subRoot.getChildIntField());
    assertSetsEqual(referenceSet, subRoot.getChildSetField());
    Assert.assertEquals("testString", subRoot.getChildStringField());

    Assert.assertEquals(5, subRoot.getParentIntField());
    assertMappings(referenceMap, subRoot.getParentMapField());
    assertIntegerArray(referenceArray, subRoot.getParentArrayField());

    barrier.barrier();
  }

  private void subClassAllFieldsUnManagedGettingUsingReflectionTest(int index) throws Exception {
    clear();

    Set referenceSet = new HashSet();
    referenceSet.add("first element");
    referenceSet.add("second element");

    Map referenceMap = new HashMap();
    referenceMap.put("first key", "first element");
    referenceMap.put("second key", "second element");

    int[] referenceArray = new int[] { 1, 2 };

    synchronized (subRoot) {
      TransparentAccess ta = (TransparentAccess) subRoot;
      if (index == 0) {
        ta.__tc_setmanagedfield("com.tctest.PhysicalInstrumentedMethodsTestApp$TestSubSubClass.grandChildIntField",
                                Integer.valueOf(2));
        ta.__tc_setmanagedfield("com.tctest.PhysicalInstrumentedMethodsTestApp$TestSubSubClass.grandChildObjectField",
                                Integer.valueOf(7));
        ta.__tc_setmanagedfield("com.tctest.PhysicalInstrumentedMethodsTestApp$TestSubClass.childIntField",
                                Integer.valueOf(4));
        ta.__tc_setmanagedfield("com.tctest.PhysicalInstrumentedMethodsTestApp$TestSubClass.childSetField",
                                referenceSet);
        ta.__tc_setmanagedfield("com.tctest.PhysicalInstrumentedMethodsTestApp$TestSubClass.childStringField",
                                "testString");
        ta.__tc_setmanagedfield("com.tctest.PhysicalInstrumentedMethodsTestApp$TestParentClass.parentIntField",
                                Integer.valueOf(5));
        ta.__tc_setmanagedfield("com.tctest.PhysicalInstrumentedMethodsTestApp$TestParentClass.parentMapField",
                                referenceMap);
        ta.__tc_setmanagedfield("com.tctest.PhysicalInstrumentedMethodsTestApp$TestParentClass.parentArrayField",
                                referenceArray);

        Map map = new HashMap();
        // __tc_getallfields() will not resolve reference.
        Class subRootClass = subRoot.getClass();

        Method m = subRootClass.getDeclaredMethod("__tc_getallfields", new Class[] { Map.class });
        m.invoke(subRoot, new Object[] { map });

        Assert
            .assertEquals(Integer.valueOf(2),
                          map.get("com.tctest.PhysicalInstrumentedMethodsTestApp$TestSubSubClass.grandChildIntField"));
        Assert.assertEquals(Integer.valueOf(7), map
            .get("com.tctest.PhysicalInstrumentedMethodsTestApp$TestSubSubClass.grandChildObjectField"));
        Assert.assertTrue(this == map.get("com.tctest.PhysicalInstrumentedMethodsTestApp$TestSubSubClass.this$0"));
        Assert.assertEquals(4, ((Integer) map
            .get("com.tctest.PhysicalInstrumentedMethodsTestApp$TestSubClass.childIntField")).intValue());
        assertSetsEqual(referenceSet,
                        (Set) map.get("com.tctest.PhysicalInstrumentedMethodsTestApp$TestSubClass.childSetField"));
        Assert.assertEquals("testString",
                            map.get("com.tctest.PhysicalInstrumentedMethodsTestApp$TestSubClass.childStringField"));

        Assert.assertEquals(5, ((Integer) map
            .get("com.tctest.PhysicalInstrumentedMethodsTestApp$TestParentClass.parentIntField")).intValue());
        assertMappings(referenceMap,
                       (Map) map.get("com.tctest.PhysicalInstrumentedMethodsTestApp$TestParentClass.parentMapField"));
        assertIntegerArray(referenceArray,
                           (int[]) map
                               .get("com.tctest.PhysicalInstrumentedMethodsTestApp$TestParentClass.parentArrayField"));

      }
    }
    barrier.barrier();
  }

  private void subClassAllFieldsUnManagedGettingTest(int index) throws Exception {
    clear();

    Set referenceSet = new HashSet();
    referenceSet.add("first element");
    referenceSet.add("second element");

    Map referenceMap = new HashMap();
    referenceMap.put("first key", "first element");
    referenceMap.put("second key", "second element");

    int[] referenceArray = new int[] { 1, 2 };

    synchronized (subRoot) {
      TransparentAccess ta = (TransparentAccess) subRoot;
      if (index == 0) {
        ta.__tc_setmanagedfield("com.tctest.PhysicalInstrumentedMethodsTestApp$TestSubSubClass.grandChildIntField",
                                Integer.valueOf(2));
        ta.__tc_setmanagedfield("com.tctest.PhysicalInstrumentedMethodsTestApp$TestSubSubClass.grandChildObjectField",
                                Integer.valueOf(7));
        ta.__tc_setmanagedfield("com.tctest.PhysicalInstrumentedMethodsTestApp$TestSubClass.childIntField",
                                Integer.valueOf(4));
        ta.__tc_setmanagedfield("com.tctest.PhysicalInstrumentedMethodsTestApp$TestSubClass.childSetField",
                                referenceSet);
        ta.__tc_setmanagedfield("com.tctest.PhysicalInstrumentedMethodsTestApp$TestSubClass.childStringField",
                                "testString");
        ta.__tc_setmanagedfield("com.tctest.PhysicalInstrumentedMethodsTestApp$TestParentClass.parentIntField",
                                Integer.valueOf(5));
        ta.__tc_setmanagedfield("com.tctest.PhysicalInstrumentedMethodsTestApp$TestParentClass.parentMapField",
                                referenceMap);
        ta.__tc_setmanagedfield("com.tctest.PhysicalInstrumentedMethodsTestApp$TestParentClass.parentArrayField",
                                referenceArray);

        Map map = new HashMap();
        // __tc_getallfields() will not resolve reference.
        ta.__tc_getallfields(map);
        Assert
            .assertEquals(Integer.valueOf(2),
                          map.get("com.tctest.PhysicalInstrumentedMethodsTestApp$TestSubSubClass.grandChildIntField"));
        Assert.assertEquals(Integer.valueOf(7), map
            .get("com.tctest.PhysicalInstrumentedMethodsTestApp$TestSubSubClass.grandChildObjectField"));
        Assert.assertTrue(this == map.get("com.tctest.PhysicalInstrumentedMethodsTestApp$TestSubSubClass.this$0"));
        Assert.assertEquals(4, ((Integer) map
            .get("com.tctest.PhysicalInstrumentedMethodsTestApp$TestSubClass.childIntField")).intValue());
        assertSetsEqual(referenceSet,
                        (Set) map.get("com.tctest.PhysicalInstrumentedMethodsTestApp$TestSubClass.childSetField"));
        Assert.assertEquals("testString",
                            map.get("com.tctest.PhysicalInstrumentedMethodsTestApp$TestSubClass.childStringField"));

        Assert.assertEquals(5, ((Integer) map
            .get("com.tctest.PhysicalInstrumentedMethodsTestApp$TestParentClass.parentIntField")).intValue());
        assertMappings(referenceMap,
                       (Map) map.get("com.tctest.PhysicalInstrumentedMethodsTestApp$TestParentClass.parentMapField"));
        assertIntegerArray(referenceArray,
                           (int[]) map
                               .get("com.tctest.PhysicalInstrumentedMethodsTestApp$TestParentClass.parentArrayField"));

      }
    }
    barrier.barrier();
  }

  private void subClassAllFieldsManagedGettingUsingReflectionTest(int index) throws Exception {
    clear();

    Set referenceSet = new HashSet();
    referenceSet.add("first element");
    referenceSet.add("second element");

    Map referenceMap = new HashMap();
    referenceMap.put("first key", "first element");
    referenceMap.put("second key", "second element");

    int[] referenceArray = new int[] { 1, 2 };

    synchronized (subRoot) {
      TransparentAccess ta = (TransparentAccess) subRoot;
      if (index == 0) {
        ta.__tc_setmanagedfield("com.tctest.PhysicalInstrumentedMethodsTestApp$TestSubSubClass.grandChildIntField",
                                Integer.valueOf(2));
        ta.__tc_setmanagedfield("com.tctest.PhysicalInstrumentedMethodsTestApp$TestSubSubClass.grandChildObjectField",
                                Integer.valueOf(7));
        ta.__tc_setmanagedfield("com.tctest.PhysicalInstrumentedMethodsTestApp$TestSubClass.childIntField",
                                Integer.valueOf(4));
        ta.__tc_setmanagedfield("com.tctest.PhysicalInstrumentedMethodsTestApp$TestSubClass.childSetField",
                                referenceSet);
        ta.__tc_setmanagedfield("com.tctest.PhysicalInstrumentedMethodsTestApp$TestSubClass.childStringField",
                                "testString");
        ta.__tc_setmanagedfield("com.tctest.PhysicalInstrumentedMethodsTestApp$TestParentClass.parentIntField",
                                Integer.valueOf(5));
        ta.__tc_setmanagedfield("com.tctest.PhysicalInstrumentedMethodsTestApp$TestParentClass.parentMapField",
                                referenceMap);
        ta.__tc_setmanagedfield("com.tctest.PhysicalInstrumentedMethodsTestApp$TestParentClass.parentArrayField",
                                referenceArray);
      }
    }

    barrier.barrier();

    Class subRootClass = subRoot.getClass();

    Method m = subRootClass.getDeclaredMethod("__tc_getmanagedfield", new Class[] { String.class });
    Assert.assertEquals(2, ((Integer) m
        .invoke(subRoot,
                new Object[] { "com.tctest.PhysicalInstrumentedMethodsTestApp$TestSubSubClass.grandChildIntField" }))
        .intValue());
    Assert
        .assertEquals(7,
                      ((Integer) m
                          .invoke(subRoot,
                                  new Object[] { "com.tctest.PhysicalInstrumentedMethodsTestApp$TestSubSubClass.grandChildObjectField" }))
                          .intValue());
    Assert.assertNotNull(m
        .invoke(subRoot, new Object[] { "com.tctest.PhysicalInstrumentedMethodsTestApp$TestSubSubClass.this$0" }));
    if (index == 0) {
      Assert.assertTrue(this == m
          .invoke(subRoot, new Object[] { "com.tctest.PhysicalInstrumentedMethodsTestApp$TestSubSubClass.this$0" }));
    }
    Assert.assertEquals(4, ((Integer) m
        .invoke(subRoot, new Object[] { "com.tctest.PhysicalInstrumentedMethodsTestApp$TestSubClass.childIntField" }))
        .intValue());
    assertSetsEqual(referenceSet,
                    (Set) m
                        .invoke(subRoot,
                                new Object[] { "com.tctest.PhysicalInstrumentedMethodsTestApp$TestSubClass.childSetField" }));
    Assert
        .assertEquals("testString", m
            .invoke(subRoot,
                    new Object[] { "com.tctest.PhysicalInstrumentedMethodsTestApp$TestSubClass.childStringField" }));
    Assert.assertEquals(5, ((Integer) m
        .invoke(subRoot,
                new Object[] { "com.tctest.PhysicalInstrumentedMethodsTestApp$TestParentClass.parentIntField" }))
        .intValue());
    assertMappings(referenceMap,
                   (Map) m
                       .invoke(subRoot,
                               new Object[] { "com.tctest.PhysicalInstrumentedMethodsTestApp$TestParentClass.parentMapField" }));
    assertIntegerArray(referenceArray,
                       (int[]) m
                           .invoke(subRoot,
                                   new Object[] { "com.tctest.PhysicalInstrumentedMethodsTestApp$TestParentClass.parentArrayField" }));

    barrier.barrier();
  }

  private void subClassAllFieldsManagedGettingTest(int index) throws Exception {
    clear();

    Set referenceSet = new HashSet();
    referenceSet.add("first element");
    referenceSet.add("second element");

    Map referenceMap = new HashMap();
    referenceMap.put("first key", "first element");
    referenceMap.put("second key", "second element");

    int[] referenceArray = new int[] { 1, 2 };
    TransparentAccess ta = (TransparentAccess) subRoot;

    synchronized (subRoot) {
      if (index == 0) {
        ta.__tc_setmanagedfield("com.tctest.PhysicalInstrumentedMethodsTestApp$TestSubSubClass.grandChildIntField",
                                Integer.valueOf(2));
        ta.__tc_setmanagedfield("com.tctest.PhysicalInstrumentedMethodsTestApp$TestSubSubClass.grandChildObjectField",
                                Integer.valueOf(7));
        ta.__tc_setmanagedfield("com.tctest.PhysicalInstrumentedMethodsTestApp$TestSubClass.childIntField",
                                Integer.valueOf(4));
        ta.__tc_setmanagedfield("com.tctest.PhysicalInstrumentedMethodsTestApp$TestSubClass.childSetField",
                                referenceSet);
        ta.__tc_setmanagedfield("com.tctest.PhysicalInstrumentedMethodsTestApp$TestSubClass.childStringField",
                                "testString");
        ta.__tc_setmanagedfield("com.tctest.PhysicalInstrumentedMethodsTestApp$TestParentClass.parentIntField",
                                Integer.valueOf(5));
        ta.__tc_setmanagedfield("com.tctest.PhysicalInstrumentedMethodsTestApp$TestParentClass.parentMapField",
                                referenceMap);
        ta.__tc_setmanagedfield("com.tctest.PhysicalInstrumentedMethodsTestApp$TestParentClass.parentArrayField",
                                referenceArray);
      }
    }

    barrier.barrier();

    Assert.assertEquals(2, ((Integer) ta
        .__tc_getmanagedfield("com.tctest.PhysicalInstrumentedMethodsTestApp$TestSubSubClass.grandChildIntField"))
        .intValue());
    Assert.assertEquals(Integer.valueOf(7), ta
        .__tc_getmanagedfield("com.tctest.PhysicalInstrumentedMethodsTestApp$TestSubSubClass.grandChildObjectField"));
    Assert.assertNotNull(ta
        .__tc_getmanagedfield("com.tctest.PhysicalInstrumentedMethodsTestApp$TestSubSubClass.this$0"));
    if (index == 0) {
      Assert.assertTrue(this == ta
          .__tc_getmanagedfield("com.tctest.PhysicalInstrumentedMethodsTestApp$TestSubSubClass.this$0"));
    }
    Assert.assertEquals(4, ((Integer) ta
        .__tc_getmanagedfield("com.tctest.PhysicalInstrumentedMethodsTestApp$TestSubClass.childIntField")).intValue());
    assertSetsEqual(referenceSet,
                    (Set) ta
                        .__tc_getmanagedfield("com.tctest.PhysicalInstrumentedMethodsTestApp$TestSubClass.childSetField"));
    Assert.assertEquals("testString", ta
        .__tc_getmanagedfield("com.tctest.PhysicalInstrumentedMethodsTestApp$TestSubClass.childStringField"));

    Assert.assertEquals(5, ((Integer) ta
        .__tc_getmanagedfield("com.tctest.PhysicalInstrumentedMethodsTestApp$TestParentClass.parentIntField"))
        .intValue());
    assertMappings(referenceMap,
                   (Map) ta
                       .__tc_getmanagedfield("com.tctest.PhysicalInstrumentedMethodsTestApp$TestParentClass.parentMapField"));
    assertIntegerArray(referenceArray,
                       (int[]) ta
                           .__tc_getmanagedfield("com.tctest.PhysicalInstrumentedMethodsTestApp$TestParentClass.parentArrayField"));

    barrier.barrier();
  }

  private void assertIntegerArray(int[] expect, int[] actual) {
    Assert.assertEquals(expect.length, actual.length);

    for (int i = 0; i < expect.length; i++) {
      Assert.assertEquals(expect[i], actual[i]);
    }
  }

  private void assertMappings(Map expect, Map actual) {
    Assert.assertEquals(expect.size(), actual.size());

    Set expectEntries = expect.entrySet();
    Set actualEntries = actual.entrySet();

    for (Iterator i = expectEntries.iterator(); i.hasNext();) {
      Entry entry = (Entry) i.next();
      Assert.assertEquals(entry.getValue(), actual.get(entry.getKey()));
    }

    for (Iterator i = actualEntries.iterator(); i.hasNext();) {
      Entry entry = (Entry) i.next();
      Assert.assertEquals(entry.getValue(), expect.get(entry.getKey()));
    }
  }

  private void assertSetsEqual(Set expectElements, Set actual) {
    Assert.assertEquals(expectElements.size(), actual.size());

    Assert.assertTrue(expectElements.containsAll(actual));
    Assert.assertTrue(actual.containsAll(expectElements));

    if (expectElements.isEmpty()) {
      Assert.assertTrue(actual.isEmpty());
    } else {
      Assert.assertFalse(actual.isEmpty());
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    TransparencyClassSpec spec = config.getOrCreateSpec(CyclicBarrier.class.getName());
    config.addWriteAutolock("* " + CyclicBarrier.class.getName() + "*.*(..)");

    String testClass = PhysicalInstrumentedMethodsTestApp.class.getName();
    spec = config.getOrCreateSpec(testClass);
    config.addIncludePattern(testClass + "$*");
    config.addIncludePattern(testClass + "$*$TestSubSubInnerClass");

    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);

    spec.addRoot("subRoot", "subRoot");
    spec.addRoot("barrier", "barrier");
  }

  @SuppressWarnings("unused")
  private static class TestParentClass {
    private int   parentIntField;
    private Map   parentMapField;
    private int[] parentArrayField;

    public TestParentClass() {
      this.parentIntField = 0;
      this.parentMapField = new HashMap();
    }

    public int getParentIntField() {
      return parentIntField;
    }

    public void setParentIntField(int parentIntField) {
      this.parentIntField = parentIntField;
    }

    public Map getParentMapField() {
      return parentMapField;
    }

    public void setParentMapField(Map parentMapField) {
      this.parentMapField = parentMapField;
    }

    public int[] getParentArrayField() {
      return parentArrayField;
    }

    public void setParentArrayField(int[] parentArrayField) {
      this.parentArrayField = parentArrayField;
    }

    public void clear() {
      this.parentIntField = 0;
      this.parentMapField.clear();
    }
  }

  @SuppressWarnings("unused")
  private static class TestSubClass extends TestParentClass {
    private int    childIntField;
    private Set    childSetField;
    private String childStringField;
    private int    index;

    public TestSubClass() {
      this.index = 0;
      this.childIntField = 0;
      this.childSetField = new HashSet();
    }

    @Override
    public void clear() {
      this.index = 0;
      this.childIntField = 0;
      this.childSetField.clear();
      super.clear();
    }

    public int getChildIntField() {
      return childIntField;
    }

    public void setChildIntField(int childIntField) {
      this.childIntField = childIntField;
    }

    public Set getChildSetField() {
      return childSetField;
    }

    public void setChildSetField(Set childSetField) {
      this.childSetField = childSetField;
    }

    public String getChildStringField() {
      return childStringField;
    }

    public void setChildStringField(String childStringField) {
      this.childStringField = childStringField;
    }

    public int getIndex() {
      return index;
    }

    public void setIndex(int index) {
      this.index = index;
    }
  }

  @SuppressWarnings("unused")
  private class TestSubSubClass extends TestSubClass {
    private int                        grandChildIntField;
    private Object                     grandChildObjectField;
    private final TestSubSubInnerClass t;

    public TestSubSubClass() {
      super();
      this.grandChildIntField = 0;
      this.grandChildObjectField = new Object();
      this.t = new TestSubSubInnerClass(10);
    }

    public int getGrandChildIntField() {
      return grandChildIntField;
    }

    public void setGrandChildIntField(int grandChildIntField) {
      this.grandChildIntField = grandChildIntField;
    }

    public Object getGrandChildObjectField() {
      return grandChildObjectField;
    }

    public void setGrandChildObjectField(Object grandChildObjectField) {
      this.grandChildObjectField = grandChildObjectField;
    }

    @Override
    public void clear() {
      this.grandChildIntField = 0;
      this.grandChildObjectField = new Object();
      super.clear();
    }

    private class TestSubSubInnerClass {
      private final long l;

      public TestSubSubInnerClass(long l) {
        this.l = l;
      }

      long getL() {
        // this method here to remove eclipse warning
        return this.l;
      }

    }
  }
}
