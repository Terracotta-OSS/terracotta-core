package com.tc.statistics.retrieval;

import org.hyperic.sigar.Mem;
import org.hyperic.sigar.Sigar;

import junit.framework.TestCase;

public class SigarUtilTest extends TestCase {

  public void testSigarInit() throws Exception{
    SigarUtil.sigarInit();
    Class sigarClass = Class.forName("org.hyperic.sigar.Sigar");
    assertEquals(sigarClass.getName(), "org.hyperic.sigar.Sigar");
  }

  public void testSigarMem() throws Exception{
    Sigar sigar = SigarUtil.newSigar();
    Mem mem = sigar.getMem();
    System.out.println("Total memory: " + mem.getTotal());
    assertTrue(mem.getTotal() > 0);
  }
}
