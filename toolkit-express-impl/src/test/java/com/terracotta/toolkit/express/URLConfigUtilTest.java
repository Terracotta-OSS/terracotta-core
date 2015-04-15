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
package com.terracotta.toolkit.express;

import junit.framework.TestCase;

/**
 * @author Alex Snaps
 */
public class URLConfigUtilTest extends TestCase {

  public void testParsesUsername() {
    assertEquals("alex", URLConfigUtil.getUsername("alex@localhost:896"));
    assertEquals("alex", URLConfigUtil.getUsername("  alex@localhost:896"));
    assertEquals("alex", URLConfigUtil.getUsername("alex@localhost:896,alex@localhost:87645"));
    assertEquals("alex", URLConfigUtil.getUsername(" alex@localhost:896,  alex@localhost:87645"));
    assertEquals("alex", URLConfigUtil.getUsername("alex@localhost:896,localhost:87645"));
    assertEquals("alex", URLConfigUtil.getUsername("localhost:896,alex@localhost:87645"));
    try {
      assertEquals("alex", URLConfigUtil.getUsername("alex@localhost:896,john@localhost:87645"));
      fail();
    } catch (AssertionError e) {
      // Expected
    }
  }
}
