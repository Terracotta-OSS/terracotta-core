/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
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
