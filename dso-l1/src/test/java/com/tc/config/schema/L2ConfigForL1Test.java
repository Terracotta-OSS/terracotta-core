/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.config.schema;

import com.tc.test.EqualityChecker;
import com.tc.test.TCTestCase;
import java.net.InetSocketAddress;
import java.net.URI;
import org.junit.Assert;

/**
 * Unit test for {@link L2ConfigForL1}.
 */
public class L2ConfigForL1Test extends TCTestCase {

  @SuppressWarnings("unused")
  public void testL2Data() throws Exception {
    try {
      new L2ConfigForL1.L2Data(InetSocketAddress.createUnresolved(null, 20));
      fail("Didn't get NPE on no host");
    } catch (NullPointerException npe) {
      // ok
    } catch (IllegalArgumentException ia) {
      // ok
    }


    L2ConfigForL1.L2Data config = new L2ConfigForL1.L2Data(InetSocketAddress.createUnresolved("foobar", 20));
    assertEquals("foobar", config.host());
    assertEquals(20, config.tsaPort());

    EqualityChecker.checkArraysForEquality(
        new Object[] {
                      new L2ConfigForL1.L2Data(InetSocketAddress.createUnresolved("foobar", 20)),
                      new L2ConfigForL1.L2Data(InetSocketAddress.createUnresolved("foobaz", 20)),
                      new L2ConfigForL1.L2Data(InetSocketAddress.createUnresolved("foobar", 2)),
                      new L2ConfigForL1.L2Data(InetSocketAddress.createUnresolved("foobar", 30)) },
        new Object[] {
                      new L2ConfigForL1.L2Data(InetSocketAddress.createUnresolved("foobar", 20)),
                      new L2ConfigForL1.L2Data(InetSocketAddress.createUnresolved("foobaz", 20)),
                      new L2ConfigForL1.L2Data(InetSocketAddress.createUnresolved("foobar", 2)),
                      new L2ConfigForL1.L2Data(InetSocketAddress.createUnresolved("foobar", 30)) });
  }

  @SuppressWarnings("unused")
  public void testL2DefaultPort() throws Exception {
    URI uri = new URI("http://localhost");
    System.out.println(uri.getHost() +":" + uri.getPort());
    L2ConfigForL1.L2Data data = new L2ConfigForL1.L2Data(InetSocketAddress.createUnresolved("localhost", 0));
    Assert.assertEquals(L2ConfigForL1.DEFAULT_PORT, data.tsaPort());
    data = new L2ConfigForL1.L2Data(InetSocketAddress.createUnresolved("localhost", 0));
    Assert.assertEquals(L2ConfigForL1.DEFAULT_PORT, data.tsaPort());
  }

}
