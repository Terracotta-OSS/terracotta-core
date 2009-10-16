/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tctest;

import com.tc.test.server.appserver.deployment.AbstractDeploymentTest;
import com.tc.test.server.appserver.deployment.GenericServer;
import com.tc.test.server.appserver.deployment.ServerTestSetup;
import com.tc.test.server.appserver.deployment.WebApplicationServer;
import com.tc.util.TcConfigBuilder;

import java.util.Date;

import junit.framework.Test;

public class CargoTest extends AbstractDeploymentTest {

  // this test is only used while testing Cargo
  public CargoTest() {
    disableAllUntil(new Date(Long.MAX_VALUE));
    GenericServer.setDsoEnabled(false);
  }

  public static Test suite() {
    return new ServerTestSetup(CargoTest.class);
  }

  public void test1Server() throws Exception {
    TcConfigBuilder config = new TcConfigBuilder();
    WebApplicationServer server = makeWebApplicationServer(config);
    server.start();
    System.out.println("App server has started. Will wait for 10s before shutting it down!");
    Thread.sleep(10 * 1000);
    server.stop();
  }

  public void test2Servers() throws Exception {
    TcConfigBuilder config = new TcConfigBuilder();
    WebApplicationServer server1 = makeWebApplicationServer(config);
    WebApplicationServer server2 = makeWebApplicationServer(config);
    server1.start();
    System.out.println("First server started successfully.");
    server2.start();
    System.out.println("Second server started successfully.");
    System.out.println("App servers has started. Will wait for 10s before shutting it down!");
    Thread.sleep(10 * 1000);
    server1.stop();
    server2.stop();
  }
}
