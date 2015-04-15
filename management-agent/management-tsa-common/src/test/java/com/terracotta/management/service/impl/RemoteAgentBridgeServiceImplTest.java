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
package com.terracotta.management.service.impl;


import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RemoteAgentBridgeServiceImplTest {

  @Test
  public void testStringArrayToString__2strings() throws Exception {
    String[] array = new String[]{"string1", "string2"};
    String s = RemoteAgentBridgeServiceImpl.toString(array);
    assertEquals("string1,string2",s);
  }

  @Test
  public void testStringArrayToString__1string() throws Exception {
    String[] array = new String[]{"string1"};
    String s = RemoteAgentBridgeServiceImpl.toString(array);
    assertEquals("string1",s);
  }

}