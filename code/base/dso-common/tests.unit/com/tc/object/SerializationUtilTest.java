/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object;


import junit.framework.Assert;
import junit.framework.TestCase;

public class SerializationUtilTest extends TestCase {

  private SerializationUtil UTIL = new SerializationUtil();
  
  public void testIsParent() {
    Assert.assertEquals(true, UTIL.isParent("this$123"));
    Assert.assertEquals(true, UTIL.isParent("this$1"));
    Assert.assertEquals(false, UTIL.isParent("this"));
    Assert.assertEquals(false, UTIL.isParent("BOGUS"));
    Assert.assertEquals(false, UTIL.isParent("athis$123"));
    Assert.assertEquals(false, UTIL.isParent("this$123x"));
    Assert.assertEquals(false, UTIL.isParent("this123"));
    Assert.assertEquals(false, UTIL.isParent("this$"));
  }
}
