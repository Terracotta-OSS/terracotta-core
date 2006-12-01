/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.test.restart;

import com.tc.objectserver.control.ServerControl;


public class RestartTestHelper {

  private final RestartTestEnvironment env;
  private final ServerCrasherConfigImpl serverCrasherConfig;
  private final boolean isCrashy;
  private final ServerControl server;
  
  public RestartTestHelper(boolean isCrashy, RestartTestEnvironment env) throws Exception {
    this.isCrashy = isCrashy;
    //env = new RestartTestEnvironment(tempDirectory, portChooser);
    this.env = env;
    serverCrasherConfig = new ServerCrasherConfigImpl();

    env.choosePorts();
    env.setIsPersistent(true);
    initRestartEnv();
    env.setUp();
    server = env.newExtraProcessServer();
    serverCrasherConfig.setServer(server);
  }

  public ServerControl getServerControl() {
    return this.server;
  }
  
  public int getServerPort() {
    return env.getServerPort();
  }
  
  private void initRestartEnv() {
    if (isCrashy) {
      initRestartEnvCrashy();
    } else {
      initRestartEnvNotCrashy();
    }
  }

  private void initRestartEnvNotCrashy() {
    System.err.println("INITIALIZING TEST AS A RESTART (CLEAN SHUTDOWN) TEST");
    env.setIsParanoid(false);
    serverCrasherConfig.setIsCrashy(false);
  }

  private void initRestartEnvCrashy() {
    System.err.println("INITIALIZING TEST AS A CRASHY (NOT CLEAN SHUTDOWN) TEST");
    env.setIsParanoid(true);
    serverCrasherConfig.setIsCrashy(true);
  }
  

  public ServerCrasherConfig getServerCrasherConfig() {
    return serverCrasherConfig;
  }
  
  public static final class ServerCrasherConfigImpl implements ServerCrasherConfig {

    private ServerControl serverControl;
    private boolean       isCrashy        = true;
    private long          restartInterval = 30 * 1000;

    public ServerCrasherConfigImpl() {
      return;
    }

    public void setServer(ServerControl serverControl) {
      this.serverControl = serverControl;
    }

    public ServerControl getServerControl() {
      return serverControl;
    }

    public void setIsCrashy(boolean b) {
      this.isCrashy = b;
    }

    public boolean isCrashy() {
      return this.isCrashy;
    }

    public void setRestartInterval(long l) {
      this.restartInterval = l;
    }

    public long getRestartInterval() {
      return this.restartInterval;
    }

  }

}
