/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.lang;

import junit.framework.TestCase;

public class TCEqualsBuilderTest extends TestCase {

  private TCEqualsBuilder eq;

  protected void setUp() throws Exception {
    super.setUp();
    eq = new TCEqualsBuilder();
  }

  public void testAppendbooleanboolean() {
    boolean v1 = true;
    boolean v2 = false;

    eq.append(v1, v1);
    assertTrue(eq.isEquals());
    eq.append(v2, v2);
    assertTrue(eq.isEquals());
    eq.append(v1, v2);
    assertFalse(eq.isEquals());
    eq.append(v2, v1);
    assertFalse(eq.isEquals());
    eq.append(v1, v1);
    assertFalse(eq.isEquals());
  }

  public void testAppendbooleanArraybooleanArray() {
    // TODO
  }

  public void testAppendbytebyte() {
    byte v1 = (byte) 1;
    byte v2 = (byte) 2;

    eq.append(v1, v1);
    assertTrue(eq.isEquals());
    eq.append(v2, v2);
    assertTrue(eq.isEquals());
    eq.append(v1, v2);
    assertFalse(eq.isEquals());
    eq.append(v2, v1);
    assertFalse(eq.isEquals());
    eq.append(v1, v1);
    assertFalse(eq.isEquals());
  }

  public void testAppendbyteArraybyteArray() {
    // TODO
  }

  public void testAppendcharchar() {
    char v1 = '1';
    char v2 = '2';

    eq.append(v1, v1);
    assertTrue(eq.isEquals());
    eq.append(v2, v2);
    assertTrue(eq.isEquals());
    eq.append(v1, v2);
    assertFalse(eq.isEquals());
    eq.append(v2, v1);
    assertFalse(eq.isEquals());
    eq.append(v1, v1);
    assertFalse(eq.isEquals());
  }

  public void testAppendcharArraycharArray() {
    // TODO
  }

  public void testAppenddoubledouble() {
    double v1 = 1.0;
    double v2 = 2.0;

    eq.append(v1, v1);
    assertTrue(eq.isEquals());
    eq.append(v2, v2);
    assertTrue(eq.isEquals());
    eq.append(v1, v2);
    assertFalse(eq.isEquals());
    eq.append(v2, v1);
    assertFalse(eq.isEquals());
    eq.append(v1, v1);
    assertFalse(eq.isEquals());
  }

  public void testAppenddoubleArraydoubleArray() {
    // TODO
  }

  public void testAppendfloatfloat() {
    float v1 = (float) 1.0;
    float v2 = (float) 2.0;

    eq.append(v1, v1);
    assertTrue(eq.isEquals());
    eq.append(v2, v2);
    assertTrue(eq.isEquals());
    eq.append(v1, v2);
    assertFalse(eq.isEquals());
    eq.append(v2, v1);
    assertFalse(eq.isEquals());
    eq.append(v1, v1);
    assertFalse(eq.isEquals());
  }

  public void testAppendfloatArrayfloatArray() {
    // TODO
  }

  public void testAppendintint() {
    int v1 = 1;
    int v2 = 2;

    eq.append(v1, v1);
    assertTrue(eq.isEquals());
    eq.append(v2, v2);
    assertTrue(eq.isEquals());
    eq.append(v1, v2);
    assertFalse(eq.isEquals());
    eq.append(v2, v1);
    assertFalse(eq.isEquals());
    eq.append(v1, v1);
    assertFalse(eq.isEquals());

  }

  public void testAppendintArrayintArray() {
    // TODO
  }

  public void testAppendlonglong() {
    long v1 = 1;
    long v2 = 2;

    eq.append(v1, v1);
    assertTrue(eq.isEquals());
    eq.append(v2, v2);
    assertTrue(eq.isEquals());
    eq.append(v1, v2);
    assertFalse(eq.isEquals());
    eq.append(v2, v1);
    assertFalse(eq.isEquals());
    eq.append(v1, v1);
    assertFalse(eq.isEquals());
  }

  public void testAppendlongArraylongArray() {
    // TODO
  }

  public void testAppendObjectObject() {
    Object v1 = new Object();
    Object v2 = new Object();

    eq.append(v1, v1);
    assertTrue(eq.isEquals());
    eq.append(v2, v2);
    assertTrue(eq.isEquals());
    eq.append(v1, v2);
    assertFalse(eq.isEquals());
    eq.append(v2, v1);
    assertFalse(eq.isEquals());
    eq.append(v1, v1);
    assertFalse(eq.isEquals());
  }

  public void testAppendObjectArrayObjectArray() {
    // TODO
  }

  public void testAppendshortshort() {
    short v1 = 1;
    short v2 = 2;

    eq.append(v1, v1);
    assertTrue(eq.isEquals());
    eq.append(v2, v2);
    assertTrue(eq.isEquals());
    eq.append(v1, v2);
    assertFalse(eq.isEquals());
    eq.append(v2, v1);
    assertFalse(eq.isEquals());
    eq.append(v1, v1);
    assertFalse(eq.isEquals());
  }

  public void testAppendshortArrayshortArray() {
    // TODO
  }

  public void testAppendSuper() {
    eq.appendSuper(true);
    assertTrue(eq.isEquals());
    eq.appendSuper(false);
    assertFalse(eq.isEquals());
    eq.appendSuper(true);
    assertFalse(eq.isEquals());
  }

}
