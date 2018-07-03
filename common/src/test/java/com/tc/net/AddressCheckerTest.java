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
package com.tc.net;

import org.junit.jupiter.api.Test;

import java.net.InetAddress;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AddressCheckerTest {

  @Test
  public void test() throws Exception {
    AddressChecker ac = new AddressChecker();

    assertTrue(ac.isLegalBindAddress(InetAddress.getByName("127.0.0.1")));
    assertTrue(ac.isLegalBindAddress(InetAddress.getByName("0.0.0.0")));
    assertTrue(ac.isLegalBindAddress(InetAddress.getLocalHost()));

    assertFalse(ac.isLegalBindAddress(InetAddress.getByName("137.254.16.113")));
  }

}
