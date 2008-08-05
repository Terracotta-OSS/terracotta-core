/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.server.appserver.deployment;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.tc.test.AppServerInfo;
import com.tc.test.TCTestCase;
import com.tc.test.TestConfigObject;
import com.tc.test.server.util.TcConfigBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import junit.framework.Test;
import junit.framework.TestSuite;

public abstract class AbstractDeploymentTest extends TCTestCase {

  protected Log         logger              = LogFactory.getLog(getClass());

  private ServerManager serverManager;

  private final Map     disabledVariants    = new HashMap();
  private final List    disabledJavaVersion = new ArrayList();

  public static Test suite() {
    return new ErrorTestSetup(new TestSuite(AbstractDeploymentTest.class));
  }

  public AbstractDeploymentTest() {
    //
  }

  public boolean shouldDisable() {
    return isAllDisabled() || shouldDisableForJavaVersion() || shouldDisableForVariants();
  }

  protected void beforeTimeout() throws Throwable {
    getServerManager().timeout();
  }

  protected boolean shouldKillAppServersEachRun() {
    return true;
  }

  protected AppServerInfo appServerInfo() {
    return TestConfigObject.getInstance().appServerInfo();
  }

  /**
   * override this method to pass extra arguments to the L2
   */
  protected Collection getExtraJvmArgsForL2() {
    return Collections.EMPTY_LIST;
  }

  public void runBare() throws Throwable {
    if (shouldDisable()) { return; }
    super.runBare();
  }

  protected ServerManager getServerManager() {
    if (serverManager == null) {
      try {
        serverManager = ServerManagerUtil.startAndBind(getClass(), isWithPersistentStore(), getExtraJvmArgsForL2());
      } catch (Exception e) {
        throw new RuntimeException("Unable to create server manager; " + e.toString(), e);
      }
    }
    return serverManager;
  }

  protected void tearDown() throws Exception {
    if (shouldKillAppServersEachRun()) {
      ServerManagerUtil.stopAllWebServers(serverManager);
    }
    super.tearDown();
  }

  /**
   * tcConfig: resource path to tc-config.xml
   */
  protected WebApplicationServer makeWebApplicationServer(String tcConfig) throws Exception {
    return getServerManager().makeWebApplicationServer(tcConfig);
  }

  protected WebApplicationServer makeWebApplicationServer(TcConfigBuilder configBuilder) throws Exception {
    return getServerManager().makeWebApplicationServer(configBuilder);
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

  protected void stopAllWebServers() {
    ServerManagerUtil.stopAllWebServers(getServerManager());
  }

  public boolean isWithPersistentStore() {
    return false;
  }

  protected final boolean cleanTempDir() {
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
