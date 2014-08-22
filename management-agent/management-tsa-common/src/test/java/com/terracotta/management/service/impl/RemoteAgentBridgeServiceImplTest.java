package com.terracotta.management.service.impl;

import junit.framework.TestCase;

public class RemoteAgentBridgeServiceImplTest extends TestCase {

  public void testStringArrayToString__2strings() throws Exception {
    String[] array = new String[]{"string1", "string2"};
    String s = RemoteAgentBridgeServiceImpl.stringArrayToString(array);
    assertEquals("string1,string2",s);
  }

  public void testStringArrayToString__1string() throws Exception {
    String[] array = new String[]{"string1"};
    String s = RemoteAgentBridgeServiceImpl.stringArrayToString(array);
    assertEquals("string1",s);
  }

}