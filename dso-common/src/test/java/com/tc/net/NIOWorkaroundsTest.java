/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.net;

import java.util.Properties;

import junit.framework.TestCase;

public class NIOWorkaroundsTest extends TestCase {

  public void testSolaris10Workaround() {
    assertFalse(NIOWorkarounds.solaris10Workaround(makeProps("sun", "SunOS", "5.10", "1.5.0_11")));
    assertFalse(NIOWorkarounds.solaris10Workaround(makeProps("sun", "SunOS", "5.10", "1.5.0_09")));
    assertFalse(NIOWorkarounds.solaris10Workaround(makeProps("sun", "SunOS", "5.10", "1.5.0_08")));
    assertTrue(NIOWorkarounds.solaris10Workaround(makeProps("sun", "SunOS", "5.10", "1.5.0_07")));
    assertTrue(NIOWorkarounds.solaris10Workaround(makeProps("sun", "SunOS", "5.10", "1.5.0")));
    assertFalse(NIOWorkarounds.solaris10Workaround(makeProps("sun", "SunOS", "5.10", "1.6.0")));
    assertFalse(NIOWorkarounds.solaris10Workaround(makeProps("sun", "SunOS", "5.10", "1.7.0")));
    assertTrue(NIOWorkarounds.solaris10Workaround(makeProps("sun", "SunOS", "5.10", "1.4.2_11")));
    assertFalse(NIOWorkarounds.solaris10Workaround(makeProps("sun", "SunOS", "5.9", "1.4.2_11")));
    assertFalse(NIOWorkarounds.solaris10Workaround(makeProps("sun", "Linux", "5.10", "1.4.2_11")));
    assertFalse(NIOWorkarounds.solaris10Workaround(makeProps("sun", "Linux", "5.10", "1.5.0_09")));
    assertFalse(NIOWorkarounds.solaris10Workaround(makeProps("bea", "SunOS", "5.10", "1.5.0_09")));
  }

  private static Properties makeProps(String vendor, String osName, String osVersion, String javaVersion) {
    Properties props = new Properties();
    props.setProperty("java.vendor", vendor);
    props.setProperty("os.name", osName);
    props.setProperty("os.version", osVersion);
    props.setProperty("java.version", javaVersion);
    return props;
  }

}
