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
package com.tc.handler;

import java.util.ResourceBundle;

public class CallbackHandlerResources {

  private static final CallbackHandlerResources instance = new CallbackHandlerResources();
  private final ResourceBundle                  resources;

  private CallbackHandlerResources() {
    this.resources = ResourceBundle.getBundle(getClass().getPackage().getName() + ".messages");
  }

  /**
   * DirtyDB Zap error messages
   */
  public static String getDirtyDBAutodeleteAutoRestartZapMessage() {
    return getErrorMessage("dirtydb.zap.autodelete.autorestart");
  }

  public static String getDirtyDBAutoRestartZapMessage() {
    return getErrorMessage("dirtydb.zap.autorestart");
  }

  public static String getDirtyDBAutodeleteZapMessage() {
    return getErrorMessage("dirtydb.zap.autodelete");
  }

  public static String getDirtyDBZapMessage() {
    return getErrorMessage("dirtydb.zap");
  }

  private static String getErrorMessage(String key) {
    return instance.resources.getString(key);
  }
}
