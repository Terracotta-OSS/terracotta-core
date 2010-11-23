/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.server;

import com.tc.config.schema.setup.L2ConfigurationSetupManager;
import com.tc.lang.TCThreadGroup;

public class StandardServerFactory extends AbstractServerFactory {
  public TCServer createServer(L2ConfigurationSetupManager configurationSetupManager, TCThreadGroup threadGroup) {
    return new TCServerImpl(configurationSetupManager, threadGroup);
  }
}
