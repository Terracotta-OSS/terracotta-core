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
package com.tc.test;


import org.junit.jupiter.api.Assertions;

/**
 * Contains a simple method that checks two arrays for equality. Checks that elements at the same index in both arrays
 * are equal; checks that all other pairs of elements are not equal. (Checks equality both ways, to make sure it's
 * properly transitive.) Also checks that hash codes are equal when objects are equal.
 */
public class EqualityChecker {

  public static void checkArraysForEquality(Object[] env1, Object[] env2) {
    checkArraysForEquality(env1, env2, true);
  }

  public static void checkArraysForEquality(Object[] env1, Object[] env2, boolean checkHashCode) {
    if (env1 == null && env2 == null) { return; }
    for (int i = 0; i < env1.length; ++i) {
      for (int j = 0; j < env2.length; ++j) {
        if (i == j) {
          Assertions.assertEquals(env1[i], env2[j]);
          if (checkHashCode) Assertions.assertEquals(env1[i].hashCode(), env2[j].hashCode());

          Assertions.assertEquals(env1[i], env1[i]);
          if (checkHashCode) Assertions.assertEquals(env1[i].hashCode(), env1[j].hashCode());

          Assertions.assertEquals(env2[i], env2[i]);
          if (checkHashCode) Assertions.assertEquals(env2[i].hashCode(), env2[j].hashCode());
        } else {
          int finalI1 = i;
          int finalJ1 = j;
          Assertions.assertFalse(env1[i].equals(env1[j]), () -> "Object in array #1 at position " + finalI1
                                                                + " is equal to the object in the same array at position " + finalJ1);
          int finalI2 = i;
          int finalJ2 = j;
          Assertions.assertFalse(env1[i].equals(env2[j]), ()->"Object in array #1 at position " + finalI1 + " is equal to object in array #2 at position " + finalJ2);
          int finalI3 = i;
          int finalJ3 = j;
          Assertions.assertFalse(env2[i].equals(env2[j]), ()->"Object in array #2 at position " + finalI3
                                       + " is equal to the object in the same array at position " + finalJ3);
        }
      }
    }
  }

}
