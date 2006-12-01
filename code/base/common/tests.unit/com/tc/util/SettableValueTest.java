/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util;

import com.tc.test.TCTestCase;

/**
 * Unit test for {@link SettableValue}.
 */
public class SettableValueTest extends TCTestCase {

  public void testBasics() throws Exception {
    SettableValue value = new SettableValue();

    assertFalse(value.isSet());
    assertNull(value.value());

    value.set(null);
    assertTrue(value.isSet());
    assertNull(value.value());

    Integer i = new Integer(3);

    value.set(i);
    assertTrue(value.isSet());
    assertSame(i, value.value());

    value.set(null);
    assertTrue(value.isSet());
    assertNull(value.value());

    value.unset();
    assertFalse(value.isSet());
    assertNull(value.value());
  }
  
  public void testDefaultValue() {
    SettableValue value = new SettableValue();
    Object actualValue = new Object();
    Object defaultValue = new Object();
    assertFalse(actualValue.equals(defaultValue));
    assertSame(defaultValue, value.value(defaultValue));
    
    value.set(actualValue);
    assertSame(actualValue, value.value(defaultValue));
    
    value.unset();
    assertSame(defaultValue, value.value(defaultValue));
    
  }

  public void testSerialization() throws Exception {
    SettableValue value = new SettableValue();
    
    SerializationTestUtil.testSerializationAndEquals(value);
  }

  public void testEquals() throws Exception {
    SettableValue[] values = new SettableValue[7];
    for (int i = 0; i < values.length; ++i)
      values[i] = new SettableValue();

    values[0].set(new Integer(5));
    values[1].set(null);
    values[3].set(new Integer(5));
    values[4].set(new Integer(6));
    values[5].set(null);

    checkEquals(values, 0, new int[] { 0, 3 });
    checkEquals(values, 1, new int[] { 1, 5 });
    checkEquals(values, 2, new int[] { 2, 6 });
    checkEquals(values, 3, new int[] { 3, 0 });
    checkEquals(values, 4, new int[] { 4 });
    checkEquals(values, 5, new int[] { 5, 1 });
    checkEquals(values, 6, new int[] { 6, 2 });

    for (int i = 0; i < values.length; ++i) {
      assertEquals(values[i], values[i].clone());
    }

    assertFalse(values[0].equals(null));
    assertFalse(values[0].equals(new Integer(5)));
    assertFalse(values[0].equals("foo"));
  }

  public void testClone() throws Exception {
    SettableValue unset = new SettableValue();
    SettableValue setToNull = new SettableValue();
    SettableValue setToValue = new SettableValue();

    setToNull.set(null);
    setToValue.set("foo");

    SettableValue unsetClone = (SettableValue) unset.clone();
    assertEquals(unset, unsetClone);
    assertFalse(unset == unsetClone);

    SettableValue setToNullClone = (SettableValue) setToNull.clone();
    assertEquals(setToNull, setToNullClone);
    assertFalse(setToNull == setToNullClone);

    SettableValue setToValueClone = (SettableValue) setToValue.clone();
    assertEquals(setToValue, setToValueClone);
    assertFalse(setToValue == setToValueClone);
  }

  private void checkEquals(SettableValue[] values, int index, int[] equalsIndices) throws Exception {
    for (int i = 0; i < values.length; ++i) {
      boolean equals = values[i].equals(values[index]);
      boolean shouldEqual = isIn(equalsIndices, i);

      assertEquals("For value " + i + ":", shouldEqual, equals);
      if (shouldEqual) {
        assertEquals("For value " + i + ":", values[index].hashCode(), values[i].hashCode());
      }
    }
  }

  private boolean isIn(int[] search, int theValue) {
    for (int i = 0; i < search.length; ++i) {
      if (search[i] == theValue) return true;
    }
    return false;
  }

}
