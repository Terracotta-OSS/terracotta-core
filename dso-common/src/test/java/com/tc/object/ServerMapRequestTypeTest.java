/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

import junit.framework.TestCase;

public class ServerMapRequestTypeTest extends TestCase {

  public void testOrdinals() {
   
    try {
      ServerMapRequestType.fromOrdinal(-1);
      fail("should fail for ordinal: " + -1);
    } catch (AssertionError e) {
      //
    }
    try {
      ServerMapRequestType.fromOrdinal(ServerMapRequestType.values().length);
      fail("should fail for ordinal: " + ServerMapRequestType.values().length);
    } catch (AssertionError e) {
      //
    }
    
    ServerMapRequestType type = ServerMapRequestType.fromOrdinal(ServerMapRequestType.GET_ALL_KEYS.ordinal());
    assertEquals(ServerMapRequestType.GET_ALL_KEYS, type);
    
    type = ServerMapRequestType.fromOrdinal(ServerMapRequestType.GET_SIZE.ordinal());
    assertEquals(ServerMapRequestType.GET_SIZE, type);
    
    type = ServerMapRequestType.fromOrdinal(ServerMapRequestType.GET_VALUE_FOR_KEY.ordinal());
    assertEquals(ServerMapRequestType.GET_VALUE_FOR_KEY, type);
  }

}
