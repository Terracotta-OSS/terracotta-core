/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.util.Assert;
import com.tc.util.runtime.Vm;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.logging.LogManager;

public class GroupManagerFactory {

  public static GroupManager createGroupManager() throws GroupException {
    // Using reflection to avoid weird 1.4 / 1.5 project dependency issues !!
    if (Vm.isJDK15Compliant()) {
      return createTribesGroupManager();
    } else {
      return new SingleNodeGroupManager();
    }
  }

  private static GroupManager createTribesGroupManager() throws GroupException {
    initLoggerForJuli();
    try {
      Class clazz = Class.forName("com.tc.net.groups.TribesGroupManager");
      Constructor constructor = clazz.getConstructor(new Class[0]);
      return (GroupManager) constructor.newInstance(new Object[0]);
    } catch (Exception e) {
      throw new GroupException(e);
    }
  }

  private static void initLoggerForJuli() {
    System.setProperty("java.util.logging.config.class", LogConfig.class.getName());
  }

  public static final class LogConfig {
    public LogConfig() throws SecurityException, IOException {
      InputStream in = GroupManagerFactory.class.getResourceAsStream("/com/tc/logging/juli.properties");
      Assert.assertNotNull(in);
      LogManager.getLogManager().readConfiguration(in);
    }
  }

}
