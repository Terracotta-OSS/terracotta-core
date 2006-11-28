/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.util;

import junit.framework.TestCase;

public class UtilTest extends TestCase {

  public void test() {
    System.out.println(Util.enumerateArray(null));
    System.out.println(Util.enumerateArray(this)); // not an array
    System.out.println(Util.enumerateArray(new Object[] {}));
    System.out.println(Util.enumerateArray(new Object[] { null, "timmy" }));
    System.out.println(Util.enumerateArray(new char[] {}));
    System.out.println(Util.enumerateArray(new long[] { 42L }));
  }

}
