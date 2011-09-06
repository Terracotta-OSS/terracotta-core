/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
