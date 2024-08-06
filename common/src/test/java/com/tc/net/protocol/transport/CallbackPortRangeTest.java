/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.net.protocol.transport;

import java.util.Iterator;
import java.util.Set;

import junit.framework.TestCase;

public class CallbackPortRangeTest extends TestCase {

  public void testRangeParse() {
    Set<Integer> ports = CallbackPortRange.expandRange("");
    assertEquals(0, ports.size());

    ports = CallbackPortRange.expandRange("  ");
    assertEquals(0, ports.size());

    ports = CallbackPortRange.expandRange("1,4,3,2");
    Iterator<Integer> iter = ports.iterator();
    assertEquals(4, ports.size());
    assertEquals(1, iter.next().intValue());
    assertEquals(4, iter.next().intValue());
    assertEquals(3, iter.next().intValue());
    assertEquals(2, iter.next().intValue());

    ports = CallbackPortRange.expandRange("1-4");
    assertEquals(4, ports.size());

    ports = CallbackPortRange.expandRange("  1, 3  - 4,2 ,  5  ");
    iter = ports.iterator();
    assertEquals(5, ports.size());
    assertEquals(1, iter.next().intValue());
    assertEquals(3, iter.next().intValue());
    assertEquals(4, iter.next().intValue());
    assertEquals(2, iter.next().intValue());
    assertEquals(5, iter.next().intValue());

    ports = CallbackPortRange.expandRange("3-4,2-4");
    iter = ports.iterator();
    assertEquals(3, ports.size());
    assertEquals(3, iter.next().intValue());
    assertEquals(4, iter.next().intValue());
    assertEquals(2, iter.next().intValue());

    ports = CallbackPortRange.expandRange("5-5");
    assertEquals(1, ports.size());
    assertEquals(5, ports.iterator().next().intValue());

    try {
      CallbackPortRange.expandRange("4-1");
      fail();
    } catch (IllegalArgumentException iae) {
      // expected
    }

    try {
      CallbackPortRange.expandRange("wazzup");
      fail();
    } catch (NumberFormatException nfe) {
      // expected
    }
  }

}
