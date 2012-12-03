/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.platform;

import com.tc.object.bytecode.Manager;
import com.tc.object.bytecode.ManagerUtil;

public final class StaticPlatformApi {

  public static void enableSingleton(Manager manager) {
    // todo: remove mgrUtil
    ManagerUtil.enableSingleton(manager);
  }
}
