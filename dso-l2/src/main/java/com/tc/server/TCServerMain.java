/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.server;

import com.tc.classloader.ServiceLocator;
import com.tc.config.schema.setup.ConfigurationSetupManagerFactory;
import com.tc.config.schema.setup.L2ConfigurationSetupManager;
import com.tc.config.schema.setup.StandardConfigurationSetupManagerFactory;
import com.tc.l2.logging.TCLogbackLogging;
import com.tc.lang.TCThreadGroup;
import com.tc.lang.ThrowableHandler;
import com.tc.lang.ThrowableHandlerImpl;

import org.slf4j.LoggerFactory;
import org.terracotta.config.service.ServiceConfigParser;

public class TCServerMain {

  public static TCServer server;
  public static L2ConfigurationSetupManager setup;

  public static void main(String[] args) {
    TCLogbackLogging.initLogging();
    ThrowableHandler throwableHandler = new ThrowableHandlerImpl(LoggerFactory.getLogger(TCServerMain.class));

    try {
      TCThreadGroup threadGroup = new TCThreadGroup(throwableHandler);

      ConfigurationSetupManagerFactory factory = new StandardConfigurationSetupManagerFactory(args,
                                                                                              StandardConfigurationSetupManagerFactory.ConfigMode.L2, null);

      ClassLoader systemLoader = ServiceLocator.getPlatformLoader();
      Thread.currentThread().setContextClassLoader(systemLoader);

//  set this as the context loader for creation of all the infrastructure at bootstrap time.

      setup = factory.createL2TVSConfigurationSetupManager(null, new ServiceClassLoader(ServiceLocator.getImplementations(ServiceConfigParser.class, systemLoader)));

      String logDir = setup.commonl2Config().logsPath().getCanonicalPath() + "/" +
                      setup.dsoL2Config().host() + "-" + setup.dsoL2Config().tsaPort().getValue();
      TCLogbackLogging.redirectLogging(logDir);

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