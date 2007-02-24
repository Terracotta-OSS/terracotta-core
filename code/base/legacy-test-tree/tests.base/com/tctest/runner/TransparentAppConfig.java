/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.runner;

import com.tc.object.config.DSOClientConfigHelper;
import com.tc.objectserver.control.ServerControl;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.app.ApplicationConfigBuilder;
import com.tc.simulator.app.GlobalIdGenerator;

import java.util.HashMap;
import java.util.Map;

public class TransparentAppConfig implements ApplicationConfig, ApplicationConfigBuilder {

  private final String            applicationClassname;
  private final GlobalIdGenerator idGenerator;
  private final Map               extraConfigAttributes             = new HashMap();
  private final ServerControl     serverControl;
  // private int globalParticipantCount;
  private int                     intensity;
  private int                     clientCount;
  private int                     applicationInstancePerClientCount = 1;

  public TransparentAppConfig(String applicationClassname, GlobalIdGenerator idGenerator, int clientCount, int intensity, ServerControl serverControl) {
    this.applicationClassname = applicationClassname;
    this.idGenerator = idGenerator;
    if (clientCount < 1) throw new AssertionError("Client count must be greater than 0");
    this.clientCount = clientCount;
    this.intensity = intensity;
    this.serverControl = serverControl;
  }

  public void setAttribute(String key, String value) {
    extraConfigAttributes.put(key, value);
  }

  public String getAttribute(String key) {
    return (String) extraConfigAttributes.get(key);
  }

  public int nextGlobalId() {
    return (int) idGenerator.nextId();
  }

  public int getGlobalParticipantCount() {
    return this.clientCount * this.applicationInstancePerClientCount;
  }

  public TransparentAppConfig setApplicationInstancePerClientCount(int applicationInstanceCount) {
    this.applicationInstancePerClientCount = applicationInstanceCount;
    return this;
  }

  public int getApplicationInstancePerClientCount() {
    return this.applicationInstancePerClientCount;
  }

  public int getClientCount() {
    return this.clientCount;
  }

  public TransparentAppConfig setClientCount(int i) {
    this.clientCount = i;
    return this;
  }

  public int getIntensity() {
    return this.intensity;
  }

  public TransparentAppConfig setIntensity(int i) {
    this.intensity = i;
    return this;
  }

  public String getApplicationClassname() {
    return this.applicationClassname;
  }

  // ApplicationConfigBuilder interface...

  public void visitClassLoaderConfig(DSOClientConfigHelper config) {
    return;
  }

  public ApplicationConfig newApplicationConfig() {
    return this;
  }

  public ApplicationConfig copy() {
    throw new AssertionError();
  }

  public ServerControl getServerControl() {
    return serverControl;
  }

}
