/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

import junit.framework.TestCase;

public class UtilHashTest extends TestCase {

  public void testHash() {
    int hashValue = Util.hash(new Integer(966330312), 10);
    Assert.assertTrue("Hashcode 966330312 return negative hash value " + hashValue, hashValue >= 0);
  }
}
