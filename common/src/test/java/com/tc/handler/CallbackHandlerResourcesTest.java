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
package com.tc.handler;

import com.tc.test.TCTestCase;
import com.tc.util.Assert;

import java.util.ResourceBundle;

public class CallbackHandlerResourcesTest extends TCTestCase {

  private ResourceBundle resources;

  @Override
  protected void setUp() throws Exception {
    this.resources = ResourceBundle.getBundle(getClass().getPackage().getName() + ".messages");
  }

  public void testResources() {
    Assert.assertTrue(CallbackHandlerResources.getDirtyDBAutodeleteAutoRestartZapMessage()
        .equals(this.resources.getObject("dirtydb.zap.autodelete.autorestart")));
    Assert.assertTrue(CallbackHandlerResources.getDirtyDBAutodeleteZapMessage()
        .equals(this.resources.getObject("dirtydb.zap.autodelete")));
    Assert.assertTrue(CallbackHandlerResources.getDirtyDBAutoRestartZapMessage()
        .equals(this.resources.getObject("dirtydb.zap.autorestart")));
    Assert.assertTrue(CallbackHandlerResources.getDirtyDBZapMessage().equals(this.resources.getObject("dirtydb.zap")));
  }
}
