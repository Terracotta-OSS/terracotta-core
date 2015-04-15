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
package com.tc.admin;

import com.tc.server.TCServerMain;
import com.tc.test.TCTestCase;
import com.tc.util.ToolClassNames;

/**
 * These tests verify that the tool class names recorded in ToolClassNames
 * are still valid against the actual class names. If someone changes the
 * class name of TCStop or AdminClient, we want these tests to go off,
 * indicating a needed change in ToolClassNames.
 */
public class ToolClassNameCheck extends TCTestCase {

  public void testTCStopClassName() {
    assertEquals(TCStop.class.getName(), ToolClassNames.TC_STOP_CLASS_NAME);
  }

  public void testTCServerClassName() {
    assertEquals(TCServerMain.class.getName(), ToolClassNames.TC_SERVER_CLASS_NAME);
  }
}
