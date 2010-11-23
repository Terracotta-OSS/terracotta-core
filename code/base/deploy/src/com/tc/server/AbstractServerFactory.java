/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.server;

import com.tc.config.schema.setup.L2ConfigurationSetupManager;
import com.tc.lang.TCThreadGroup;
import com.tc.util.factory.AbstractFactory;

public abstract class AbstractServerFactory extends AbstractFactory {
  private static String FACTORY_SERVICE_ID            = "com.tc.server.ServerFactory";
  private static Class  STANDARD_SERVER_FACTORY_CLASS = StandardServerFactory.class;

  public static AbstractServerFactory getFactory() {
    return (AbstractServerFactory) getFactory(FACTORY_SERVICE_ID, STANDARD_SERVER_FACTORY_CLASS);
  }

  public abstract TCServer createServer(L2ConfigurationSetupManager configurationSetupManager,
                                        TCThreadGroup threadGroup);
}
