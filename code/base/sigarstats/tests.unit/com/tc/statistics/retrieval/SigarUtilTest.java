package com.tc.statistics.retrieval;

import junit.framework.TestCase;

public class SigarUtilTest extends TestCase {

  public void testSigarInit() throws Exception{
    SigarUtil.sigarInit();
    Class sigarClass = Class.forName("org.hyperic.sigar.Sigar");
    assertEquals(sigarClass.getName(), "org.hyperic.sigar.Sigar");
  }
}
