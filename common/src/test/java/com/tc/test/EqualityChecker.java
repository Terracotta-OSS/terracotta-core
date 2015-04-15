/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.test;

import junit.framework.Assert;

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
          Assert.assertEquals(env1[i], env2[j]);
          if (checkHashCode) Assert.assertEquals(env1[i].hashCode(), env2[j].hashCode());

          Assert.assertEquals(env1[i], env1[i]);
          if (checkHashCode) Assert.assertEquals(env1[i].hashCode(), env1[j].hashCode());

          Assert.assertEquals(env2[i], env2[i]);
          if (checkHashCode) Assert.assertEquals(env2[i].hashCode(), env2[j].hashCode());
        } else {
          Assert.assertFalse("Object in array #1 at position " + i
                             + " is equal to the object in the same array at position " + j, env1[i].equals(env1[j]));
          Assert
              .assertFalse("Object in array #1 at position " + i + " is equal to object in array #2 at position " + j,
                           env1[i].equals(env2[j]));
          Assert.assertFalse("Object in array #2 at position " + i
                             + " is equal to the object in the same array at position " + j, env2[i].equals(env2[j]));
        }
      }
    }
  }

}