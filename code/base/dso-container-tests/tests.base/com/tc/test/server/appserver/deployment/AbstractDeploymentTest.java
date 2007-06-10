/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.server.appserver.deployment;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.tc.test.TCTestCase;
import com.tc.test.TestConfigObject;
import com.tc.test.server.appserver.NewAppServerFactory;
import com.tc.test.server.tcconfig.StandardTerracottaAppServerConfig;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public abstract class AbstractDeploymentTest extends TCTestCase {

  protected Log            logger              = LogFactory.getLog(getClass());

  private ServerManager    serverManager;
  private WatchDog         watchDog;

  Map                      disabledVariants    = new HashMap();
  List                     disabledJavaVersion = new ArrayList();

  private static final int TIMEOUT_DEFAULT     = 30 * 60;

  public AbstractDeploymentTest() {
    // nop
  }

  protected boolean isSessionTest() {
    return true;
  }

  public void runBare() throws Throwable {
    getServerManager();
    if (shouldDisable()) { return; }

    watchDog = new WatchDog(getTimeout());
    try {
      watchDog.startWatching();
      super.runBare();
    } finally {
      watchDog.stopWatching();
    }
  }

  protected ServerManager getServerManager() {
    if (serverManager == null) {
      try {
        serverManager = ServerManagerUtil.startAndBind(getClass(), isWithPersistentStore());
      } catch (Exception e) {
        throw new RuntimeException("Unable to create server manager; " + e.toString(), e);
      }
    }
    return serverManager;
  }

  protected int getTimeout() throws IOException {
    String timeout = TestConfigObject.getInstance().springTestsTimeout();
    if (timeout == null) {
      return TIMEOUT_DEFAULT;
    } else {
      return Integer.parseInt(timeout);
    }
  }

  protected void tearDown() throws Exception {
    ServerManagerUtil.stop(serverManager);
    super.tearDown();
  }

  protected WebApplicationServer makeWebApplicationServer(String tcConfig) throws Exception {
    return getServerManager().makeWebApplicationServer(tcConfig);
  }

  protected WebApplicationServer makeWebApplicationServer(FileSystemPath tcConfigPath) throws Exception {
    return getServerManager().makeWebApplicationServer(tcConfigPath);
  }

  protected WebApplicationServer makeWebApplicationServer(StandardTerracottaAppServerConfig tcConfig) throws Exception {
    return getServerManager().makeWebApplicationServer(tcConfig);
  }

  protected void restartDSO() throws Exception {
    getServerManager().restartDSO(isWithPersistentStore());
  }

  protected DeploymentBuilder makeDeploymentBuilder(String warFileName) {
    return getServerManager().makeDeploymentBuilder(warFileName);
  }

  // XXX: This causes the bad war file name which breaks WLS tests
  // protected DeploymentBuilder makeDeploymentBuilder() throws IOException {
  // return serverManager.makeDeploymentBuilder();
  // }

  protected void waitForSuccess(int timeoutInSeconds, TestCallback callback) throws Throwable {
    long startingTime = System.currentTimeMillis();
    long timeout = timeoutInSeconds * 1000;
    while (true) {
      try {
        logger.debug("checking");
        callback.check();
        logger.debug("check passed");
        return;
      } catch (Throwable e) {
        logger.debug("check failed");
        if ((System.currentTimeMillis() - startingTime) >= timeout) {
          logger.debug("check timed out", e);
          throw e;
        }
      }
      logger.debug("check sleeping");
      Thread.sleep(100L);
    }
  }

  protected StandardTerracottaAppServerConfig getConfigBuilder() {
    return getServerManager().getConfig();
  }

  protected void stopAllWebServers() {
    ServerManagerUtil.stopAllWebServers(getServerManager());
  }

  public boolean isWithPersistentStore() {
    return false;
  }

  protected void disableVariant(String variantName, String variantValue) {
    List variantList = (List) disabledVariants.get(variantName);
    if (variantList == null) {
      variantList = new ArrayList();
      disabledVariants.put(variantName, variantList);
    }
    variantList.add(variantValue);
  }

  protected void disableForJavaVersion(String version) {
    this.disabledJavaVersion.add(version);
  }

  void disableAllTests() {
    this.disableAllUntil(new Date(Long.MAX_VALUE));
  }

  public boolean shouldDisable() {
    return shouldDisableForJavaVersion() || shouldDisableForVariants() || shouldDisableForCertainAppServers();
  }

  private boolean shouldDisableForCertainAppServers() {
    String appserver = TestConfigObject.getInstance().appserverFactoryName();
    // XXX: Only non-session container tests work in glassfish and jetty at the moment
    if (isSessionTest()
        && (NewAppServerFactory.GLASSFISH.equals(appserver) || NewAppServerFactory.JETTY.equals(appserver))) { return true; }
    return false;
  }

  private boolean shouldDisableForVariants() {
    for (Iterator iter = disabledVariants.entrySet().iterator(); iter.hasNext();) {
      Map.Entry entry = (Map.Entry) iter.next();
      String variantName = (String) entry.getKey();
      List variants = (List) entry.getValue();
      String selected = getServerManager().getTestConfig().selectedVariantFor(variantName);
      if (variants.contains(selected)) {
        logger.warn("Test " + getName() + " is disabled for " + variantName + " = " + selected);
        return true;
      }
    }
    return false;
  }

  private boolean shouldDisableForJavaVersion() {
    String currentVersion = System.getProperties().getProperty("java.version");
    for (Iterator iter = disabledJavaVersion.iterator(); iter.hasNext();) {
      String version = (String) iter.next();
      if (currentVersion.matches(version)) {
        logger.warn("Test " + getName() + " is disabled for " + version);
        return true;
      }
    }
    return false;
  }

}
