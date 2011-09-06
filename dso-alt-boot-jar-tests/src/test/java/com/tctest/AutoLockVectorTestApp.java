/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

import com.tc.object.config.ConfigLockLevel;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Stack;
import java.util.Vector;

public class AutoLockVectorTestApp extends GenericTransparentApp {

  public AutoLockVectorTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider, Vector.class);
  }

  protected Object getTestObject(String testName) {
    List lists = (List) sharedMap.get("lists");
    return lists.iterator();
  }

  protected void setupTestObject(String testName) {
    List lists = new ArrayList();
    lists.add(new Vector());
    lists.add(new Stack());

    sharedMap.put("lists", lists);
    sharedMap.put("arrayforVector", new Object[4]);
    sharedMap.put("arrayforStack", new Object[4]);
  }

  private void initialize(Vector vector) {
    vector.addAll(getInitialData());
  }

  private List getInitialData() {
    List data = new ArrayList();
    data.add("January");
    data.add("February");
    data.add("March");
    data.add("April");
    return data;
  }

  void testAddAll(Vector vector, boolean validate) {
    if (validate) {
      assertListsEqual(getInitialData(), vector);
    } else {
      initialize(vector);
    }
  }

  void testAdd(Vector vector, boolean validate) {
    if (validate) {
      assertListsEqual(getInitialData(), vector);
    } else {
      vector.add("January");
      vector.add("February");
      vector.add("March");
      vector.add("April");
    }
  }

  void testClear(Vector vector, boolean validate) {
    if (validate) {
      Assert.assertEquals(0, vector.size());
    } else {
      initialize(vector);
      assertListsEqual(getInitialData(), vector);
      vector.clear();
    }
  }

  void testIteratorRemove(Vector vector, boolean validate) {
    if (validate) {
      List expect = getInitialData();
      Iterator expectIterator = expect.iterator();
      expectIterator.next();
      expectIterator.next();
      expectIterator.remove();
      assertListsEqual(expect, vector);
    } else {
      initialize(vector);
      Iterator iterator = vector.iterator();
      iterator.next();
      iterator.next();
      iterator.remove();
    }
  }

  void testListIteratorAdd(Vector vector, boolean validate) {
    if (validate) {
      List expect = getInitialData();
      expect.add(2, "May");
      assertListsEqual(expect, vector);
    } else {
      initialize(vector);
      ListIterator iterator = vector.listIterator();
      iterator.next();
      iterator.next();
      iterator.add("May");
    }
  }

  void testListIteratorRemove(Vector vector, boolean validate) {
    if (validate) {
      List expect = getInitialData();
      expect.remove(1);
      assertListsEqual(expect, vector);
    } else {
      initialize(vector);
      ListIterator iterator = vector.listIterator();
      iterator.next();
      iterator.next();
      iterator.remove();
    }
  }

  void testRemove(Vector vector, boolean validate) {
    if (validate) {
      List expect = getInitialData();
      expect.remove("February");
      assertListsEqual(expect, vector);
    } else {
      initialize(vector);
      vector.remove("February");
    }
  }

  void testRemoveAt(Vector vector, boolean validate) {
    if (validate) {
      List expect = getInitialData();
      expect.remove("February");
      assertListsEqual(expect, vector);
    } else {
      initialize(vector);
      vector.remove(1);
    }
  }

  void testSetSizeGrow(Vector vector, boolean validate) {
    int size = 5;

    if (validate) {
      Assert.assertEquals("January", vector.get(0));
      for (int i = 1; i < size; i++) {
        Object val = vector.get(i);
        Assert.assertNull("element " + i + " is " + val, val);
      }
      Assert.assertEquals("July", vector.get(size));
    } else {
      vector.add("January");
      vector.setSize(size);
      vector.add("July");
    }
  }

  void testSetSizeShrink(Vector vector, boolean validate) {
    if (validate) {
      Assert.assertEquals("January", vector.get(0));
      Assert.assertEquals("February", vector.get(1));
    } else {
      vector.add("January");
      vector.add("ho hum");
      vector.add("ho hum2");
      vector.add("ho hum3");
      vector.setSize(1);
      vector.add("February");
    }
  }

  void testAddElement(Vector vector, boolean validate) {
    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { "January", "February" }), vector);
    } else {
      vector.addElement("January");
      vector.addElement("February");
    }
  }

  void testRetainAll(Vector vector, boolean validate) {
    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { "February", "April" }), vector);
    } else {
      initialize(vector);
      List toRetain = new ArrayList();
      toRetain.add("February");
      toRetain.add("April");
      vector.retainAll(toRetain);
    }
  }

  void testRemoveAll(Vector vector, boolean validate) {
    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { "January", "March" }), vector);
    } else {
      initialize(vector);
      List toRemove = new ArrayList();
      toRemove.add("February");
      toRemove.add("April");
      vector.removeAll(toRemove);
    }
  }

  void testListIteratorSet(Vector vector, boolean validate) {
    if (validate) {
      List expect = getInitialData();
      expect.set(1, "February modified");
      assertListsEqual(expect, vector);
    } else {
      initialize(vector);
      ListIterator iterator = vector.listIterator();
      iterator.next();
      iterator.next();
      iterator.set("February modified");
    }
  }

  void testToArray(Vector vector, boolean validate) {
    Object[] array = getArray(vector);

    if (validate) {
      assertListsEqual(Arrays.asList(array), vector);
    } else {
      initialize(vector);
      synchronized (array) {
        Object[] returnArray = vector.toArray(array);
        Assert.assertTrue(returnArray == array);
      }
    }
  }
  
  void testCopyInto(Vector vector, boolean validate) {
    Object[] array = getArray(vector);

    if (validate) {
      assertListsEqual(Arrays.asList(array), vector);
    } else {
      initialize(vector);
      synchronized (array) {
        vector.copyInto(array);
      }
    }
  }

  void testSetElementAt(Vector vector, boolean validate) {
    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { "January", "February Modified", "March", "April" }), vector);
    } else {
      initialize(vector);
      vector.setElementAt("February Modified", 1);
    }
  }

  void testSubList(Vector vector, boolean validate) {
    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { "January", "May", "February", "March", "April" }), vector);
    } else {
      initialize(vector);
      List subList = vector.subList(1, 2);
      ListIterator listIterator = subList.listIterator();
      listIterator.add("May");
    }
  }
  
  void testElements(Vector vector, boolean validate) {
    if (validate) {
      List expect = getInitialData();
      int i = 0;
      Enumeration elements = vector.elements();
      while (elements.hasMoreElements()) {
        Object expectObj = expect.get(i++);
        Object actualObj = elements.nextElement();
        Assert.assertEquals(expectObj, actualObj);
      }
    } else {
      initialize(vector);
    }
  }

  void testStackPush(Vector vector, boolean validate) {
    if (!(vector instanceof Stack)) { return; }

    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { "January", "February" }), vector);
    } else {
      Stack s = (Stack) vector;
      s.push("January");
      s.push("February");
    }
  }

  void testStackPop(Vector vector, boolean validate) {
    if (!(vector instanceof Stack)) { return; }

    if (validate) {
      assertListsEqual(Arrays.asList(new Object[] { "January" }), vector);
    } else {
      Stack s = (Stack) vector;
      s.push("January");
      s.push("February");

      Object o = s.pop();
      Assert.assertEquals("February", o);
    }
  }

  private Object[] getArray(Vector vector) {
    if (vector instanceof Stack) { return (Object[]) sharedMap.get("arrayforStack"); }
    return (Object[]) sharedMap.get("arrayforVector");
  }

  private void assertListsEqual(List expect, List actual) {
    Assert.assertEquals(expect.size(), actual.size());

    Assert.assertTrue(expect.containsAll(actual));
    Assert.assertTrue(actual.containsAll(expect));

    for (int i = 0, n = expect.size(); i < n; i++) {
      Assert.assertEquals(expect.get(i), actual.get(i));
    }

    if (expect.isEmpty()) {
      Assert.assertTrue(actual.isEmpty());
    } else {
      Assert.assertFalse(actual.isEmpty());
    }

    for (Iterator iExpect = expect.iterator(), iActual = actual.iterator(); iExpect.hasNext();) {
      Assert.assertEquals(iExpect.next(), iActual.next());
    }
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClass = AutoLockVectorTestApp.class.getName();
    config.getOrCreateSpec(testClass);
    
    config.addIncludePattern("java.util.Vector");
    config.addAutolock("* java.util.Vector.*(..)", ConfigLockLevel.WRITE);
  }

}
