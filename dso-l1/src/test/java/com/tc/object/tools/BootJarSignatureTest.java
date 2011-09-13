/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.tools;

import java.util.Properties;

import junit.framework.TestCase;

public class BootJarSignatureTest extends TestCase {

  public void testExceptions() {
    try {
      new BootJarSignature(new Properties());
      fail();
    } catch (Exception uve) {
      // expected
    }

    try {
      new BootJarSignature(makeProps(null, "Sun", "1.5.0_04", null, "i686"));
      fail();
    } catch (Exception uve) {
      // expected
    }

    try {
      new BootJarSignature(makeProps("Windows NT", null, "1.5.0_04", null, "i686"));
      fail();
    } catch (Exception uve) {
      // expected
    }

    try {
      new BootJarSignature(makeProps("Windows NT", "1.4.2_04", null, null, "i686"));
      fail();
    } catch (Exception uve) {
      // expected
    }
  }

  public void testSolaris() throws UnsupportedVMException {
    Properties props = makeProps("SunOS", "Sun Microsystems Inc.", "1.5.0_06", null, "sparc");
    BootJarSignature sig = new BootJarSignature(props);
    assertEquals("hotspot_solaris_150", sig.getSignature());

    props = makeProps("SunOS", "Sun Microsystems Inc.", "1.4.2_12", null, "x86");
    sig = new BootJarSignature(props);
    assertEquals("hotspot_solaris-x86_142", sig.getSignature());

    try {
      props = makeProps("SunOS", "Sun Microsystems Inc.", "1.5.0_06", null, null);
      new BootJarSignature(props);
      fail();
    } catch (UnsupportedVMException uve) {
      // expected (since os.arch was null)
    }

  }

  public void testWindows() throws UnsupportedVMException {
    Properties props = makeProps("Windows XP", "Sun Microsystems Inc.", "1.5.0_06", null, null);
    BootJarSignature sig = new BootJarSignature(props);
    assertEquals("hotspot_win32_150", sig.getSignature());

    props = makeProps("Windows 2000", "Sun Microsystems Inc.", "1.4.2_12", null, null);
    sig = new BootJarSignature(props);
    assertEquals("hotspot_win32_142", sig.getSignature());
  }

  public void testUnknown() throws UnsupportedVMException {
    Properties props = makeProps("Joe bob's OS", "Sun Microsystems Inc.", "1.5.0_06", null, null);
    BootJarSignature sig = new BootJarSignature(props);
    assertEquals("hotspot_unknown_150", sig.getSignature());

    props = makeProps("Windows XP", "hot dog vendor", "1.6.0_23", null, null);
    sig = new BootJarSignature(props);
    assertEquals("unknown_win32_160", sig.getSignature());
  }

  public void testMac() throws UnsupportedVMException {
    Properties props = makeProps("Mac OS X", "Apple Computer, Inc.", "1.5.0_05", null, null);
    BootJarSignature sig = new BootJarSignature(props);
    assertEquals("hotspot_osx_150", sig.getSignature());
  }

  public void testLinux() throws UnsupportedVMException {
    Properties props = makeProps("Linux", "Sun Microsystems, Inc.", "1.5.0_01", null, null);
    BootJarSignature sig = new BootJarSignature(props);
    assertEquals("hotspot_linux_150", sig.getSignature());

    props = makeProps("Linux", "BEA Systems, Inc.", "1.5.0_03", null, null);
    sig = new BootJarSignature(props);
    assertEquals("jrockit_linux_150", sig.getSignature());

    props = makeProps("Linux", "IBM Corporation", "1.5.0", "pxi32dev-20070201 (SR4)", null);
    sig = new BootJarSignature(props);
    assertEquals("ibm_linux_150", sig.getSignature());

    // experimental version identifiers in case these should pop up one day
    props = makeProps("Linux", "IBM Corporation, Inc.", "1.5.0_11", "pxi32dev-20070201 (SR4)", null);
    sig = new BootJarSignature(props);
    assertEquals("ibm_linux_150", sig.getSignature());

    // test this exceptional case
    props = makeProps("Linux", "Sun Microsystems, Inc.", "1.4.2_05", null, null);
    props.setProperty("java.vm.name", "BEA WebLogic JRockit(TM) 1.4.2_05 JVM R24.5.0-0");
    props.setProperty("jrockit.version", "ari-41062-20050215-0919-linux-ia32");
    sig = new BootJarSignature(props);
    assertEquals("jrockit_linux_142", sig.getSignature());
  }

  private Properties makeProps(String os, String vendor, String version, String runtime, String arch) {
    Properties props = new Properties();
    if (version != null) props.put("java.version", version);
    if (os != null) props.put("os.name", os);
    if (arch != null) props.put("os.arch", arch);
    if (runtime != null) props.put("java.runtime.version", runtime);
    if (vendor != null) props.put("java.vendor", vendor);
    return props;
  }

}
