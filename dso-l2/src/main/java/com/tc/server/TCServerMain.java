/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.server;

import com.tc.config.schema.setup.ConfigurationSetupManagerFactory;
import com.tc.config.schema.setup.L2ConfigurationSetupManager;
import com.tc.config.schema.setup.StandardConfigurationSetupManagerFactory;
import com.tc.lang.TCThreadGroup;
import com.tc.lang.ThrowableHandler;
import com.tc.lang.ThrowableHandlerImpl;
import com.tc.logging.TCLogging;

public class TCServerMain {
  
  public static TCServer server;
  public static L2ConfigurationSetupManager setup;

  public static void main(String[] args) {
    ThrowableHandler throwableHandler = new ThrowableHandlerImpl(TCLogging.getLogger(TCServerMain.class));

    try {
      TCThreadGroup threadGroup = new TCThreadGroup(throwableHandler);

      ConfigurationSetupManagerFactory factory = new StandardConfigurationSetupManagerFactory(args,
                                                                                              StandardConfigurationSetupManagerFactory.ConfigMode.L2, null);
      setup = factory.createL2TVSConfigurationSetupManager(null);
      server = ServerFactory.createServer(setup,threadGroup);
      server.start();

      server.waitUntilShutdown();

    } catch (Throwable t) {
      throwableHandler.handleThrowable(Thread.currentThread(), t);
    }
  }
  
  public static TCServer getServer() {
    return server;
  }
  
  public static L2ConfigurationSetupManager getSetupManager() {
    return setup;
  }
}