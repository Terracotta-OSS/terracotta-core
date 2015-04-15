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
package com.tc.util.runtime;

import junit.framework.TestCase;

/**
 * Test to see if this class is valid for JRE 1-6 environment
 */
public class VmJdk16Test extends TestCase {

  public void testValidateJdk16() {
    if (Vm.isJDK16()) {
      assertFalse(Vm.isJDK14());
      assertFalse(Vm.isJDK15());
      assertTrue(Vm.isJDK15Compliant());
      assertTrue(Vm.isJDK16());
      assertTrue(Vm.isJDK16Compliant());
      assertFalse(Vm.isJDK17());
    }
  }

}
