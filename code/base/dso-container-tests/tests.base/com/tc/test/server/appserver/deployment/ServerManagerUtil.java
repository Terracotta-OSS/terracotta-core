/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.server.appserver.deployment;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Collection;

public class ServerManagerUtil {

  protected final static Log logger = LogFactory.getLog(ServerManagerUtil.class);
  private static ServerManager theServerManager;

  private static synchronized ServerManager start(Class testClass, boolean withPersistentStore, Collection extraJvmArgs,
                                     final boolean coresident) throws Exception {
    if (!coresident) {
      ServerManager existingServerManager = getExistingServerManager();
      if (existingServerManager != null) {
        logger.debug("Using existing ServerManager");
        return existingServerManager;
      }
    }
    logger.debug("Creating server manager");
    ServerManager serverManager = new ServerManager(testClass, extraJvmArgs);
    serverManager.start(withPersistentStore);
    return serverManager;
  }

  private static synchronized void stop(ServerManager serverManager, final boolean coresident) {
    if (!coresident) {
      ServerManager existingServerManager = getExistingServerManager();
      if (existingServerManager != null) {
        logger.debug("Not stopping existing ServerManager");
        return;
      }
    }
    logger.debug("Stopping ServerManager");
    serverManager.stop();
  }

  private static synchronized ServerManager getExistingServerManager() {
    return theServerManager;
  }

  public static synchronized ServerManager startAndBind(Class testClass, boolean withPersistentStore, Collection extraJvmArgs)
      throws Exception {
    return startAndBind(testClass, withPersistentStore, extraJvmArgs, false);
  }

  public static synchronized ServerManager startAndBind(Class testClass, boolean withPersistentStore, Collection extraJvmArgs,
                                           final boolean coresident) throws Exception {
    ServerManager sm = start(testClass, withPersistentStore, extraJvmArgs, coresident);
    if (!coresident) theServerManager = sm;
    return sm;
  }

  public static synchronized void stopAndRelease(ServerManager sm) {
    stopAndRelease(sm, false);
  }

  public static synchronized void stopAndRelease(ServerManager sm, final boolean coresident) {
    if (!coresident) theServerManager = null;
    stop(sm, coresident);
  }

  public static synchronized void stopAllWebServers(ServerManager serverManager) {
    stopAllWebServers(serverManager, false);
  }

  public static synchronized void stopAllWebServers(ServerManager serverManager, final boolean coresident) {
    if (!coresident) getExistingServerManager().stopAllWebServers();
    else serverManager.stopAllWebServers();
  }

}
