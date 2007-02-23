/**
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Checks that the VM that the buildsystem thinks it's running the tests with is the VM that it's *actually* running the
 * tests with.
 */
public class CorrectJVMTestBase extends TCTestCase {

  private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+\\.\\d+\\.\\d+(_\\d+)?)(-\\S+)?");

  public void testVersion() throws Exception {
    String actualVersion = System.getProperty("java.runtime.version");
    String expectedVersion = TestConfigObject.getInstance().jvmVersion();

    Matcher matcher = VERSION_PATTERN.matcher(actualVersion);

    System.err.println("Actual JVM version: '" + actualVersion + "'; expected JVM version: '" + expectedVersion + "'");

    assertTrue("Actual version of '" + actualVersion + "' matches pattern", matcher.matches());
    assertEquals(expectedVersion, matcher.group(1));
  }

  public void testType() throws Exception {
    String vmName = System.getProperty("java.vm.name").toLowerCase();
    String expectedType = TestConfigObject.getInstance().jvmType().trim().toLowerCase();

    System.err.println("Actual JVM type: '" + vmName + "'; expected JVM type: '" + expectedType + "'");

    assertTrue("Actual type of '" + vmName + "' includes expected type of '" + expectedType + "'", vmName
        .indexOf(expectedType) >= 0);
  }

  public void testMode() throws Exception {
    String vmName = System.getProperty("java.vm.name");
    String actualMode = vmName.toLowerCase().indexOf("server") >= 0 ? "server" : "client";
    String expectedMode = TestConfigObject.getInstance().jvmMode();

    System.err.println("java.vm.name=" + vmName);
    System.err.println("Actual JVM mode (from '" + vmName + "'): '" + actualMode + "'; expected JVM mode: '"
                       + expectedMode + "'");

    assertTrue("Actual mode (from '" + vmName + "') of '" + actualMode + "' includes expected mode of '" + expectedMode
               + "'", actualMode.indexOf(expectedMode) >= 0 || vmName.indexOf("64-Bit") >= 0);
  }

}
