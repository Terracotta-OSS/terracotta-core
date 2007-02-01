/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.integrationtests.framework;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.tc.process.LinkedJavaProcessPollingAgent;
import com.tc.test.TCTestCase;
import com.tc.test.TestConfigObject;
import com.tc.test.server.tcconfig.StandardTerracottaAppServerConfig;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractDeploymentTest extends TCTestCase {

  protected Log            logger          = LogFactory.getLog(getClass());

  protected ServerManager    serverManager;
  private WatchDog         watchDog;

  Map disabledVariants = new HashMap();
  List disabledJavaVersion = new ArrayList();

  private static final int TIMEOUT_DEFAULT = 30 * 60;

  protected void setUp() throws Exception {
    super.setUp();
    serverManager = ServerManagerUtil.start(getClass(), isWithPersistentStore());
  }

  public void runBare() throws Throwable {
    watchDog = new WatchDog(getTimeout());
    try {
      watchDog.startWatching();
      super.runBare();
    } finally {
      watchDog.stopWatching();
    }
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
    try {
      Thread.sleep(5000);
    } catch (InterruptedException e) {
      // ignore
    }
    LinkedJavaProcessPollingAgent.destroy();
    try {
      Thread.sleep(5000);
    } catch (InterruptedException e) {
      // ignore
    }
    super.tearDown();
  }

  protected WebApplicationServer makeWebApplicationServer(String tcConfig) throws Exception {
    return serverManager.makeWebApplicationServer(tcConfig);
  }

  protected WebApplicationServer makeWebApplicationServer(FileSystemPath tcConfigPath) throws Exception {
    return serverManager.makeWebApplicationServer(tcConfigPath);
  }

  protected WebApplicationServer makeWebApplicationServer(StandardTerracottaAppServerConfig tcConfig) throws Exception {
    return serverManager.makeWebApplicationServer(tcConfig);
  }

  protected void restartDSO() throws Exception {
    serverManager.restartDSO(isWithPersistentStore());
  }

  protected DeploymentBuilder makeDeploymentBuilder(String warFileName) {
    return serverManager.makeDeploymentBuilder(warFileName);
  }

// XXX: This causes the bad war file name which breaks WLS tests  
//  protected DeploymentBuilder makeDeploymentBuilder() throws IOException {
//    return serverManager.makeDeploymentBuilder();
//  }

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
    return serverManager.getConfig();
  }

  protected void stopAllWebServers() {
    ServerManagerUtil.stopAllWebServers(serverManager);
  }
  
  public boolean isWithPersistentStore() {
    return false;
  }
  
  protected void disableVariant(String variantName, String variantValue) {
    List variantList = (List)disabledVariants.get(variantName);
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
}
