/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.server;

import com.tc.config.schema.setup.L2TVSConfigurationSetupManager;
import com.tc.lang.TCThreadGroup;

public class StandardServerFactory extends AbstractServerFactory {
  public TCServer createServer(L2TVSConfigurationSetupManager configurationSetupManager, TCThreadGroup threadGroup) {
    return new TCServerImpl(configurationSetupManager, threadGroup);
  }
}
