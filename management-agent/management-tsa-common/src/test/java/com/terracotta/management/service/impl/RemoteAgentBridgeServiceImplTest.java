package com.terracotta.management.service.impl;


import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RemoteAgentBridgeServiceImplTest {

  @Test
  public void testStringArrayToString__2strings() throws Exception {
    String[] array = new String[]{"string1", "string2"};
    String s = RemoteAgentBridgeServiceImpl.toString(array);
    assertEquals("string1,string2",s);
  }

  @Test
  public void testStringArrayToString__1string() throws Exception {
    String[] array = new String[]{"string1"};
    String s = RemoteAgentBridgeServiceImpl.toString(array);
    assertEquals("string1",s);
  }

}