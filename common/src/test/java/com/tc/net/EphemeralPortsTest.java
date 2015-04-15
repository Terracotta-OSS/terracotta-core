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

import com.tc.net.EphemeralPorts.Range;

import junit.framework.TestCase;

public class EphemeralPortsTest extends TestCase {

  public void test() {
    Range range = EphemeralPorts.getRange();
    System.out.println(range);
    assertTrue("lower is " + range.getLower(), range.getLower() >= 1024);
    assertTrue("upper is " + range.getUpper(), range.getUpper() <= 65535);
  }

}
