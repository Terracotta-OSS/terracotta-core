/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.util;

import java.net.Socket;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ClassUtilsTest {

  @Test
  public void testArrayMethods() {
    assertEquals(int.class, ClassUtils.baseComponentType(int[][][][][].class));
    assertEquals(Object.class, ClassUtils.baseComponentType(Object[].class));

    try {
      ClassUtils.baseComponentType(null);
      fail();
    } catch (NullPointerException e) {
      // expected
    }

    try {
      ClassUtils.baseComponentType(int.class);
      fail();
    } catch (IllegalArgumentException e) {
      // expected
    }

    assertEquals(5, ClassUtils.arrayDimensions(int[][][][][].class));
    assertEquals(1, ClassUtils.arrayDimensions(Object[].class));

    try {
      ClassUtils.arrayDimensions(null);
      fail();
    } catch (NullPointerException e) {
      // expected
    }

    try {
      ClassUtils.arrayDimensions(int.class);
      fail();
    } catch (IllegalArgumentException e) {
      // expected
    }

  }

  @Test
  public void testIsPrimitiveArray() {
    assertTrue(ClassUtils.isPrimitiveArray(new byte[0]));
    assertTrue(ClassUtils.isPrimitiveArray(new boolean[1]));
    assertTrue(ClassUtils.isPrimitiveArray(new char[2]));
    assertTrue(ClassUtils.isPrimitiveArray(new double[3]));
    assertTrue(ClassUtils.isPrimitiveArray(new float[4]));
    assertTrue(ClassUtils.isPrimitiveArray(new int[5]));
    assertTrue(ClassUtils.isPrimitiveArray(new long[6]));
    assertTrue(ClassUtils.isPrimitiveArray(new short[7]));

    assertFalse(ClassUtils.isPrimitiveArray(new Object[0]));
    assertFalse(ClassUtils.isPrimitiveArray(new ClassUtilsTest[42]));
    assertFalse(ClassUtils.isPrimitiveArray(new Socket[][] { {} }));
    assertFalse(ClassUtils.isPrimitiveArray(new byte[][] { {} }));

    assertFalse(ClassUtils.isPrimitiveArray(null));
    assertFalse(ClassUtils.isPrimitiveArray(new Object()));
  }

}
