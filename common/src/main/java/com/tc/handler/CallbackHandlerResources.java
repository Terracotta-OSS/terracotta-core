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

  private static String getErrorMessage(final String key) {
    return instance.resources.getString(key);
  }
}
