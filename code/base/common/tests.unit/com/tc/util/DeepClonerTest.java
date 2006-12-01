/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import com.tc.test.TCTestCase;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

/**
 * Unit test for {@link DeepCloner}.
 */
public class DeepClonerTest extends TCTestCase {

  public void testNull() {
    assertNull(DeepCloner.deepClone(null));
  }

  public void testValueTypes() {
    Object[] valueObjects = new Object[] { new String("foo"), new Boolean(true), new Character('V'),
        new Byte((byte) -57), new Short((short) 12345), new Integer(-234892352), new Long(4912840983148025L),
        new Float(5.234789e+17), new Double(-2.48925023854e-138), new Date() };

    for (int i = 0; i < valueObjects.length; ++i) {
      assertSame(valueObjects[i], DeepCloner.deepClone(valueObjects[i]));
    }
  }

  public void testNonDeepCloneable() {
    try {
      DeepCloner.deepClone(new NonDeepCloneable());
      fail("Didn't get UOE on non-deep-cloneable");
    } catch (UnsupportedOperationException uoe) {
      // ok
    }
  }

  private static class NonDeepCloneable {
    // nothing here
  }

  private static class MyObject implements DeepCloneable, Comparable {
    private final String identity;
    private final String equalsComparison;
    private Object       reference;

    public MyObject(String identity, String equalsComparison) {
      this.identity = identity;
      this.equalsComparison = equalsComparison;
    }

    public MyObject(MyObject source, DeepCloner cloner) {
      cloner.setClone(source, this);
      this.identity = source.identity;
      this.equalsComparison = source.equalsComparison;
      this.reference = cloner.subClone(source.reference);
    }

    public void setReference(Object reference) {
      this.reference = reference;
    }

    public String identity() {
      return this.identity;
    }

    public String equalsComparison() {
      return this.equalsComparison;
    }

    public Object reference() {
      return this.reference;
    }

    public Object deepClone(DeepCloner cloner) {
      return new MyObject(this, cloner);
    }

    public boolean equals(Object that) {
      if (!(that instanceof MyObject)) return false;

      MyObject myThat = (MyObject) that;
      return new EqualsBuilder().append(this.equalsComparison, myThat.equalsComparison).isEquals();
    }

    public int hashCode() {
      return new HashCodeBuilder().append(this.equalsComparison).toHashCode();
    }

    public int compareTo(Object o) {
      return this.equalsComparison.compareTo(((MyObject) o).equalsComparison);
    }
  }

  private static class MySubObject extends MyObject {
    private Object subReference;

    public MySubObject(String identity, String equalsComparison) {
      super(identity, equalsComparison);
    }

    public MySubObject(MySubObject source, DeepCloner cloner) {
      super(source, cloner);
      this.subReference = cloner.subClone(source.subReference);
    }

    public Object subReference() {
      return this.subReference;
    }

    public Object deepClone(DeepCloner cloner) {
      return new MySubObject(this, cloner);
    }

    public void setSubReference(Object subReference) {
      this.subReference = subReference;
    }
  }

  // This class is broken: it does not correctly override deepClone(DeepCloner).
  private static class MyBrokenSubObject extends MyObject {
    private Object subReference;

    public MyBrokenSubObject(String identity, String equalsComparison) {
      super(identity, equalsComparison);
    }

    public MyBrokenSubObject(MyObject source, DeepCloner cloner) {
      super(source, cloner);
    }

    public void setSubReference(Object subReference) {
      this.subReference = subReference;
    }

    public Object subReference() {
      return this.subReference;
    }
  }

  private static class MyBrokenSetCloneObject implements DeepCloneable {
    private Object reference;

    public MyBrokenSetCloneObject() {
      this.reference = null;
    }

    private MyBrokenSetCloneObject(MyBrokenSetCloneObject source, DeepCloner cloner) {
      // Deliberate reversal of arguments here
      cloner.setClone(this, source);
      this.reference = cloner.subClone(source);
    }

    public void setReference(Object reference) {
      this.reference = reference;
    }

    public Object reference() {
      return this.reference;
    }

    public Object deepClone(DeepCloner cloner) {
      return new MyBrokenSetCloneObject(this, cloner);
    }
  }

  private static class MyNoSetCloneObject implements DeepCloneable {
    private Object reference;

    public MyNoSetCloneObject() {
      this.reference = null;
    }

    private MyNoSetCloneObject(MyNoSetCloneObject source, DeepCloner cloner) {
      this.reference = cloner.subClone(source);
    }

    public void setReference(Object reference) {
      this.reference = reference;
    }

    public Object reference() {
      return this.reference;
    }

    public Object deepClone(DeepCloner cloner) {
      return new MyNoSetCloneObject(this, cloner);
    }
  }

  public void testCircularReference() {
    MyObject one = new MyObject("a", "b");
    MyObject two = new MyObject("c", "d");

    one.setReference(two);
    two.setReference(one);

    MyObject oneClone = (MyObject) DeepCloner.deepClone(one);
    assertNotSame(one, oneClone);
    assertNotSame(two, oneClone);

    assertEquals(one, oneClone);
    assertSame(one.identity(), oneClone.identity());
    assertSame(one.equalsComparison(), oneClone.equalsComparison());

    MyObject twoClone = (MyObject) oneClone.reference();

    assertNotSame(one, twoClone);
    assertNotSame(two, twoClone);

    assertEquals(two, twoClone);
    assertSame(two.identity(), twoClone.identity());
    assertSame(two.equalsComparison(), twoClone.equalsComparison());

    assertSame(oneClone, twoClone.reference());
  }

  public void testBrokenSetClone() {
    MyBrokenSetCloneObject one = new MyBrokenSetCloneObject();
    MyBrokenSetCloneObject two = new MyBrokenSetCloneObject();

    one.setReference(two);
    two.setReference(one);

    try {
      DeepCloner.deepClone(one);
      fail("Didn't get TCAE on broken setClone() method.");
    } catch (TCAssertionError tcae) {
      // ok
    }
  }
  
  public void testNoSetClone() {
    MyNoSetCloneObject one = new MyNoSetCloneObject();
    MyNoSetCloneObject two = new MyNoSetCloneObject();
    
    one.setReference(two);
    two.setReference(one);
    
    try {
      DeepCloner.deepClone(one);
      fail("Didn't get TCAE on missing setClone() method.");
    } catch (TCAssertionError tcae) {
      // ok
    }
  }

  public void testSimpleClone() {
    MyObject obj = new MyObject("a", "b");
    obj.setReference(new Integer(432));

    MyObject clone = (MyObject) DeepCloner.deepClone(obj);

    assertNotSame(obj, clone);
    assertEquals(obj, clone);
    assertSame(obj.identity(), clone.identity());
    assertSame(obj.equalsComparison(), clone.equalsComparison());
    assertSame(obj.reference(), clone.reference());
  }

  public void testClonesReference() {
    MyObject obj = new MyObject("a", "b");
    MyObject referredTo = new MyObject("c", "d");
    obj.setReference(referredTo);
    referredTo.setReference(new Integer(543));

    MyObject clone = (MyObject) DeepCloner.deepClone(obj);

    assertNotSame(obj, clone);
    assertEquals(obj, clone);
    assertSame(obj.identity(), clone.identity());
    assertSame(obj.equalsComparison(), clone.equalsComparison());

    MyObject referredToClone = (MyObject) clone.reference();
    assertSame(obj.reference(), referredTo);
    assertNotSame(referredTo, referredToClone);
    assertEquals(referredTo, referredToClone);
    assertSame(referredTo.identity(), referredToClone.identity());
    assertSame(referredTo.equalsComparison(), referredToClone.equalsComparison());
    assertSame(referredTo.reference(), referredToClone.reference());
  }

  public void testDoesNotMultiplyCloneObjects() {
    // We also give all of these the same equals comparison; this makes sure that we're cloning objects (or returning
    // references to already-cloned objects) based on identity, not Object.equals().
    MySubObject a = new MySubObject("a", "x");
    MyObject b = new MyObject("b", "x");
    MyObject c = new MyObject("c", "x");

    a.setReference(b);
    a.setSubReference(c);
    b.setReference(c);

    MySubObject aClone = (MySubObject) DeepCloner.deepClone(a);
    MyObject bClone = (MyObject) aClone.reference();
    MyObject cCloneThroughA = (MyObject) aClone.subReference();
    MyObject cCloneThroughB = (MyObject) bClone.reference();

    assertNotSame(a, aClone);
    assertNotSame(b, bClone);
    assertNotSame(c, cCloneThroughA);
    assertNotSame(c, cCloneThroughB);

    assertSame(cCloneThroughA, cCloneThroughB);
    assertEquals(a, aClone);
    assertEquals(b, bClone);
    assertEquals(c, cCloneThroughA);

    assertSame(a.identity(), aClone.identity());
    assertSame(a.equalsComparison(), aClone.equalsComparison());
    assertSame(b.identity(), bClone.identity());
    assertSame(b.equalsComparison(), bClone.equalsComparison());
    assertSame(c.identity(), cCloneThroughA.identity());
    assertSame(c.equalsComparison(), cCloneThroughA.equalsComparison());
  }

  public void testBrokenDeepClone() {
    try {
      DeepCloner.deepClone(new MyBrokenSubObject("a", "b"));
      fail("Didn't get TCAE on broken deepClone() method");
    } catch (TCAssertionError tcae) {
      // ok
    }
  }

  public void testCollections() throws Exception {
    Collection[] collections = new Collection[] { new LinkedList(), new ArrayList(), new HashSet(),
        new LinkedHashSet(), new Vector() };

    for (int i = 0; i < collections.length; ++i) {
      checkCollection(collections[i], false);
    }

    checkCollection(new TreeSet(), true);
  }

  public void testMaps() throws Exception {
    checkMap(new HashMap(), false);
    checkMap(new TreeMap(), true);
    checkMap(new Hashtable(), false);
  }

  public void testArrays() throws Exception {
    // In DSO instrumentation of Array.get(), if the array is a primitive boolean array, Array.get()
    // will always return the same Boolean object, i.e., either Boolean.TRUE or Boolean.FALSE.
    checkPrimitiveArray(new boolean[] { true, false, false, true, false }, false);
    checkPrimitiveArray(new char[] { 'a', 'x', 'Q' }, true);
    checkPrimitiveArray(new byte[] { (byte) 43, (byte) -24, (byte) 14 }, true);
    checkPrimitiveArray(new short[] { (short) 4382, (short) -4257, (short) 1442 }, true);
    checkPrimitiveArray(new int[] { 432945, -248925, 48932525 }, true);
    checkPrimitiveArray(new long[] { 4832908402L, -2384908209348092L, 896085049582L }, true);
    checkPrimitiveArray(new float[] { 4.24983e+12f, -4.2438925e-14f, 1.89384205e+3f }, true);
    checkPrimitiveArray(new double[] { 1.890810432480e+173, 2.489032842e-132, 8.32849729523e+200 }, true);
  }

  public void testObjectArray() throws Exception {
    MySubObject a = new MySubObject("a", "a1");
    MyObject b = new MyObject("b", "b1");
    MyObject c = new MyObject("c", "c1");
    MyObject d = new MyObject("d", "d1");

    a.setReference(b);
    a.setSubReference(c);

    Integer integer = new Integer(12345);
    String string = new String("foo");

    LinkedList collection = new LinkedList();
    collection.add(a);
    collection.add(d);
    collection.add(integer);

    Object[] original = new Object[] { a, collection, string };
    Object[] cloned = (Object[]) DeepCloner.deepClone(original);

    assertEquals(original.length, cloned.length);

    MySubObject aCloneThroughArray = (MySubObject) cloned[0];
    MyObject bClone = (MyObject) aCloneThroughArray.reference();
    MyObject cClone = (MyObject) aCloneThroughArray.subReference();
    LinkedList collectionClone = (LinkedList) cloned[1];
    String stringClone = (String) cloned[2];
    MySubObject aCloneThroughCollection = (MySubObject) collectionClone.get(0);
    MyObject dClone = (MyObject) collectionClone.get(1);
    Integer integerClone = (Integer) collectionClone.get(2);

    assertNotSame(a, aCloneThroughArray);
    assertSame(a.identity(), aCloneThroughArray.identity());
    assertSame(a.equalsComparison(), aCloneThroughArray.equalsComparison());

    assertNotSame(b, bClone);
    assertSame(b.identity(), bClone.identity());
    assertSame(b.equalsComparison(), bClone.equalsComparison());

    assertNotSame(c, cClone);
    assertSame(c.identity(), cClone.identity());
    assertSame(c.equalsComparison(), cClone.equalsComparison());

    assertNotSame(collection, collectionClone);
    assertEquals(collection.size(), collectionClone.size());

    assertSame(aCloneThroughArray, aCloneThroughCollection);

    assertNotSame(d, dClone);
    assertSame(d.identity(), dClone.identity());
    assertSame(d.equalsComparison(), dClone.equalsComparison());

    assertSame(string, stringClone);
    assertSame(integer, integerClone);
  }

  private void checkPrimitiveArray(Object array, boolean differentIdentityCheckRequired) {
    Object clone = DeepCloner.deepClone(array);

    assertNotSame(array, clone);
    assertEquals(Array.getLength(array), Array.getLength(clone));

    for (int i = 0; i < Array.getLength(array); ++i) {
      Object originalValue = Array.get(array, i);
      Object clonedValue = Array.get(clone, i);

      assertEquals(originalValue, clonedValue);
      if (differentIdentityCheckRequired) {
        assertNotSame(originalValue, clonedValue);
      }
    }
  }

  private void checkMap(Map map, boolean requiresOrder) {
    MyObject aKey = new MyObject("k", "k1");
    MyObject aKeyRef = new MyObject("ak", "ak1");
    aKey.setReference(aKeyRef);

    MyObject bKey = new MyObject("b", "b1");
    bKey.setReference(aKeyRef);

    MyObject cKey = new MyObject("c", "c1");

    MySubObject a = new MySubObject("a", "b");
    MyObject ref = new MyObject("ref", "ref1");
    MyObject subRef = new MyObject("subRef", "subRef1");
    Integer integer = new Integer(42);
    String string = new String("foo");

    a.setReference(ref);
    a.setSubReference(subRef);

    LinkedList list = new LinkedList();
    list.add(ref);
    list.add(string);

    map.put(aKey, a);
    if (!requiresOrder) {
      map.put(bKey, list);
      map.put(cKey, integer);
    }

    Map cloned = (Map) DeepCloner.deepClone(map);

    assertNotSame(map, cloned);

    Iterator iter = cloned.entrySet().iterator();
    MyObject aCloneRef = null;
    boolean haveA = false, haveB = false, haveC = false;

    while (iter.hasNext()) {
      Map.Entry entry = (Map.Entry) iter.next();
      MyObject key = (MyObject) entry.getKey();
      Object value = entry.getValue();

      if (key.equals(aKey)) {
        assertNotSame(aKey, key);
        assertSame(aKey.identity(), key.identity());
        assertSame(aKey.equalsComparison(), key.equalsComparison());

        MyObject keyRef = (MyObject) key.reference();
        assertNotSame(aKeyRef, keyRef);
        assertSame(aKeyRef.identity(), keyRef.identity());
        assertSame(aKeyRef.equalsComparison(), keyRef.equalsComparison());

        MySubObject aClone = (MySubObject) value;
        MyObject thisACloneRef = (MyObject) aClone.reference();
        if (aCloneRef != null) assertSame(aCloneRef, thisACloneRef);
        else aCloneRef = thisACloneRef;
        MyObject aCloneSubRef = (MyObject) aClone.subReference();

        assertNotSame(a, aClone);
        assertSame(a.identity(), aClone.identity());
        assertSame(a.equalsComparison(), aClone.equalsComparison());

        assertNotSame(ref, aCloneRef);
        assertSame(ref.identity(), aCloneRef.identity());
        assertSame(ref.equalsComparison(), aCloneRef.equalsComparison());
        assertSame(ref.reference(), aCloneRef.reference());

        assertNotSame(subRef, aCloneSubRef);
        assertSame(subRef.identity(), aCloneSubRef.identity());
        assertSame(subRef.equalsComparison(), aCloneSubRef.equalsComparison());
        assertSame(subRef.reference(), aCloneSubRef.reference());

        haveA = true;
      } else if (key.equals(bKey)) {
        assertNotSame(bKey, key);
        assertSame(bKey.identity(), key.identity());
        assertSame(bKey.equalsComparison(), key.equalsComparison());

        LinkedList listClone = (LinkedList) value;
        MyObject thisACloneRef = (MyObject) listClone.get(0);
        if (aCloneRef != null) assertSame(aCloneRef, thisACloneRef);
        else aCloneRef = thisACloneRef;
        assertSame(string, list.get(1));

        haveB = true;
      } else if (key.equals(cKey)) {
        assertNotSame(cKey, key);
        assertSame(cKey.identity(), key.identity());
        assertSame(cKey.equalsComparison(), key.equalsComparison());

        assertSame(integer, value);

        haveC = true;
      } else {
        fail("Unknown key: " + key);
      }
    }

    assertTrue(haveA);
    assertEquals(!requiresOrder, haveB);
    assertEquals(!requiresOrder, haveC);
  }

  private void checkCollection(Collection collection, boolean requiresOrder) {
    MySubObject a = new MySubObject("a", "b");
    MyObject ref = new MyObject("ref", "ref1");
    MyObject subRef = new MyObject("subRef", "subRef1");
    Integer integer = new Integer(42);
    String string = new String("foo");

    a.setReference(ref);
    a.setSubReference(subRef);

    LinkedList list = new LinkedList();
    list.add(ref);
    list.add(string);

    collection.add(a);
    if (!requiresOrder) {
      // LinkedLists aren't Comparable, and Integers (obviously) really don't like being compared to anything but
      // Integers.
      collection.add(list);
      collection.add(integer);
    }

    Collection cloned = (Collection) DeepCloner.deepClone(collection);

    assertNotSame(collection, cloned);

    Iterator clonedIter = cloned.iterator();
    MySubObject clonedA = null;
    LinkedList clonedList = null;
    Integer clonedInteger = null;

    while (clonedIter.hasNext()) {
      Object next = clonedIter.next();
      if (next instanceof MySubObject) {
        assertNull(clonedA);
        clonedA = (MySubObject) next;
      } else if (next instanceof LinkedList) {
        assertNull(clonedList);
        clonedList = (LinkedList) next;
      } else if (next instanceof Integer) {
        assertNull(clonedInteger);
        clonedInteger = (Integer) next;
      } else {
        fail("Unknown object in collection: " + next);
      }
    }

    assertNotSame(a, clonedA);
    if (!requiresOrder) {
      assertNotSame(list, clonedList);
      assertSame(integer, clonedInteger);
    }

    MyObject clonedRef = (MyObject) clonedA.reference();
    MyObject clonedSubRef = (MyObject) clonedA.subReference();

    assertNotSame(ref, clonedRef);
    assertSame(ref.identity(), clonedRef.identity());
    assertSame(ref.equalsComparison(), clonedRef.equalsComparison());
    assertNull(ref.reference());

    assertNotSame(subRef, clonedSubRef);
    assertSame(subRef.identity(), clonedSubRef.identity());
    assertSame(subRef.equalsComparison(), clonedSubRef.equalsComparison());
    assertNull(subRef.reference());

    if (!requiresOrder) {
      assertSame(clonedRef, clonedList.get(0));
      assertSame(string, clonedList.get(1));
      assertEquals(2, clonedList.size());
    }
  }

}
